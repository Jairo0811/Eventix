package com.jairomatias.eventix.sale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGateway;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.role.repository.RoleRepository;
import com.jairomatias.eventix.sale.dto.SaleActionForm;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.PaymentRejectedException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

/**
 * Comprueba el comportamiento, no la configuración: cuando la pasarela rechaza
 * un reembolso, la transacción rechazada debe quedar confirmada en la base de
 * datos para auditoría, y la venta debe seguir pagada.
 *
 * <p>Complementa a {@code RefundTransactionRollbackPolicyTest}, que solo
 * verifica por reflexión que la anotación declara {@code noRollbackFor}. Aquí
 * el método de prueba no es transaccional a propósito: el servicio gestiona su
 * propia transacción y la lectura posterior ocurre en otra distinta, que es la
 * única forma de distinguir una fila confirmada de una revertida.
 */
@SpringBootTest
@ActiveProfiles("test")
class DeclinedRefundPersistenceIntegrationTest {

    private static final String TAG = "declined-refund";
    private static final String ADMIN = "declined.refund.admin@eventix.local";
    private static final String BUYER = "declined.refund.buyer@eventix.local";
    private static final BigDecimal PRICE = new BigDecimal("1500.00");

    @MockitoBean private PaymentGatewayRegistry gatewayRegistry;

    @Autowired private SaleService saleService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TicketTypeRepository ticketTypeRepository;
    @Autowired private PaymentTransactionRepository paymentRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventCategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private Long saleId;
    private Long eventId;
    private Long reservationId;
    private Long ticketTypeId;

    @BeforeEach
    void createPaidSaleWithApprovedCharge() {
        // PaymentGateway declara supports() y process(), así que no admite lambda.
        PaymentGateway decliningGateway = new PaymentGateway() {

            @Override
            public boolean supports(PaymentProvider provider) {
                return true;
            }

            @Override
            public PaymentResult process(PaymentCommand command) {
                return new PaymentResult(
                        PaymentStatus.DECLINED,
                        "EXT-DECLINED-1",
                        "Fondos insuficientes");
            }
        };
        when(gatewayRegistry.resolve(any(PaymentProvider.class))).thenReturn(decliningGateway);

        transactionTemplate.executeWithoutResult(status -> {
            User admin = saveUser("Admin", TAG, ADMIN, RoleName.ADMINISTRATOR);
            User buyer = saveUser("Comprador", TAG, BUYER, RoleName.USER);

            // Se reutiliza una categoría sembrada: crear una nueva rompería
            // flywaySeedsCurrentEventCategories, que exige el conjunto exacto.
            EventCategory category = categoryRepository
                    .findAllByActiveTrueOrderByNameAsc()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "El perfil de pruebas debe tener categorías sembradas."));

            Event event = eventRepository.save(new Event(
                    TAG + " evento",
                    "Evento para la prueba de reembolso rechazado",
                    category,
                    EventStatus.DRAFT,
                    LocalDateTime.now().plusDays(45),
                    LocalDateTime.now().plusDays(45).plusHours(2),
                    "Recinto de prueba",
                    "Dirección de prueba",
                    50,
                    admin,
                    null,
                    false,
                    PRICE));

            TicketType ticketType = ticketTypeRepository.save(new TicketType(
                    event,
                    TicketTypeCategory.GENERAL,
                    TAG + " general",
                    PRICE,
                    50));

            Reservation reservation = reservationRepository.save(new Reservation(
                    TAG.toUpperCase(java.util.Locale.ROOT) + "-R1",
                    event,
                    "Comprador",
                    TAG,
                    BUYER,
                    "8090000001",
                    1,
                    LocalDateTime.now().plusDays(1),
                    buyer));

            Sale sale = new Sale(TAG.toUpperCase(java.util.Locale.ROOT) + "-S1", reservation, "DOP", admin);
            sale.addItem(ticketType, 1, PRICE);
            sale.markPaid(LocalDateTime.now().minusDays(1));
            Sale savedSale = saleRepository.save(sale);

            paymentRepository.save(new PaymentTransaction(
                    savedSale,
                    TAG.toUpperCase(java.util.Locale.ROOT) + "-CHARGE-1",
                    PaymentProvider.CARDNET,
                    PaymentTransactionType.CHARGE,
                    PaymentStatus.APPROVED,
                    savedSale.getTotal(),
                    savedSale.getCurrency(),
                    "EXT-CHARGE-1",
                    "Cobro aprobado",
                    LocalDateTime.now().minusDays(1),
                    admin));

            this.saleId = savedSale.getId();
            this.eventId = event.getId();
            this.reservationId = reservation.getId();
            this.ticketTypeId = ticketType.getId();
        });
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRATOR")
    void aDeclinedRefundKeepsItsAuditTransactionCommitted() {
        SaleActionForm form = new SaleActionForm();
        form.setReason("Prueba de rechazo de la pasarela");

        assertThatThrownBy(() -> saleService.refund(saleId, form, ADMIN))
                .as("El rechazo de la pasarela debe propagarse al llamante")
                .isInstanceOf(PaymentRejectedException.class);

        List<PaymentTransaction> transactions = readCommittedTransactions();

        assertThat(transactions)
                .as("La transacción de reembolso rechazada debe sobrevivir a la excepción")
                .anySatisfy(transaction -> {
                    assertThat(transaction.getTransactionType())
                            .isEqualTo(PaymentTransactionType.REFUND);
                    assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.DECLINED);
                });

        assertThat(readCommittedSaleStatus())
                .as("Un reembolso rechazado no puede dejar la venta como reembolsada")
                .isEqualTo(SaleStatus.PAID);
    }

    private List<PaymentTransaction> readCommittedTransactions() {
        return transactionTemplate.execute(status ->
                paymentRepository.findAllBySale_IdOrderByProcessedAtDesc(saleId));
    }

    private SaleStatus readCommittedSaleStatus() {
        return transactionTemplate.execute(status ->
                saleRepository.findById(saleId).orElseThrow().getStatus());
    }

    private User saveUser(String firstName, String lastName, String email, RoleName roleName) {
        return userRepository.save(new User(
                firstName,
                lastName,
                email,
                email,
                "{noop}fixture-not-used-for-login",
                "8090000000",
                roleRepository.findByName(roleName).orElseThrow()));
    }

    @AfterEach
    void removeFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            paymentRepository.deleteAll(
                    paymentRepository.findAllBySale_IdOrderByProcessedAtDesc(saleId));
            saleRepository.findById(saleId).ifPresent(saleRepository::delete);
            reservationRepository.findById(reservationId).ifPresent(reservationRepository::delete);
            ticketTypeRepository.findById(ticketTypeId).ifPresent(ticketTypeRepository::delete);
            eventRepository.findById(eventId).ifPresent(eventRepository::delete);
            for (String login : List.of(ADMIN, BUYER)) {
                userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                        .ifPresent(userRepository::delete);
            }
        });
    }
}
