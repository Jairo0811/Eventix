package com.jairomatias.eventix.sale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.payment.dto.PaymentForm;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentGateway;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.promotion.service.PromotionService;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.sale.dto.SaleActionForm;
import com.jairomatias.eventix.sale.dto.SaleForm;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.event.SaleRefundedEvent;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DefaultSaleServiceTest {

    private static final String OPERATOR_LOGIN = "operator@eventix.local";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 1, 10, 0);

    @Mock private SaleRepository saleRepository;
    @Mock private SaleItemRepository saleItemRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private PaymentTransactionRepository paymentRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentGatewayRegistry gatewayRegistry;
    @Mock private TransactionReferenceGenerator referenceGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PromotionService promotionService;
    @Mock private PaymentGateway gateway;
    @Mock private User operator;
    @Mock private User reservedBy;
    @Mock private Event event;

    private DefaultSaleService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-01T14:00:00Z"),
                ZoneId.of("America/Santo_Domingo"));
        service = new DefaultSaleService(
                saleRepository,
                saleItemRepository,
                ticketTypeRepository,
                paymentRepository,
                reservationRepository,
                eventRepository,
                userRepository,
                gatewayRegistry,
                referenceGenerator,
                eventPublisher,
                promotionService,
                "DOP",
                clock);
    }

    @Test
    void createsPendingSaleWithPriceSnapshot() {
        Reservation reservation = prepareSaleableReservation(2);
        TicketType general = ticketType(
                31L,
                "General",
                "500.00",
                100);
        TicketType vip = ticketType(
                32L,
                "VIP",
                "1000.00",
                20);
        when(ticketTypeRepository.findDetailedByIdForUpdate(31L))
                .thenReturn(Optional.of(general));
        when(ticketTypeRepository.findDetailedByIdForUpdate(32L))
                .thenReturn(Optional.of(vip));
        when(referenceGenerator.generateSaleReference())
                .thenReturn("SAL-ABCDEFGH2345");
        when(saleRepository.existsByReferenceCode("SAL-ABCDEFGH2345"))
                .thenReturn(false);
        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale sale = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sale, "id", 55L);
                    return sale;
                });

        SaleForm form = saleForm(10L, 31L, 1);
        form.getItems().get(1).setTicketTypeId(32L);
        form.getItems().get(1).setQuantity(1);

        Long saleId = service.create(form, OPERATOR_LOGIN);

        assertThat(saleId).isEqualTo(55L);
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(captor.capture());
        Sale sale = captor.getValue();
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.PENDING);
        assertThat(sale.getItems()).hasSize(2);
        assertThat(sale.getTotal()).isEqualByComparingTo("1500.00");
        verify(eventPublisher, never()).publishEvent(any());
        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void rejectsSaleWhenItemQuantityDoesNotMatchReservation() {
        prepareSaleableReservation(3);

        assertThatThrownBy(() -> service.create(
                saleForm(10L, 31L, 2),
                OPERATOR_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 cupos");
    }

    @Test
    void approvedPaymentMarksSalePaidAndPublishesEvent() {
        Sale sale = preparePendingSale();
        PaymentForm form = paymentForm(SimulationOutcome.APPROVE);
        when(gatewayRegistry.resolve(PaymentProvider.AZUL))
                .thenReturn(gateway);
        when(gateway.process(any()))
                .thenReturn(new PaymentResult(
                        PaymentStatus.APPROVED,
                        "SIM-APPROVED",
                        "Aprobado"));
        preparePaymentReference();

        boolean approved = service.processPayment(
                55L,
                form,
                OPERATOR_LOGIN);

        assertThat(approved).isTrue();
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.PAID);
        verify(eventPublisher).publishEvent(new SalePaidEvent(55L));
        verify(promotionService).consumeForSale(55L, NOW);
        verify(paymentRepository).save(any(PaymentTransaction.class));
    }

    @Test
    void declinedPaymentLeavesSalePending() {
        Sale sale = preparePendingSale();
        PaymentForm form = paymentForm(SimulationOutcome.DECLINE);
        when(gatewayRegistry.resolve(PaymentProvider.AZUL))
                .thenReturn(gateway);
        when(gateway.process(any()))
                .thenReturn(new PaymentResult(
                        PaymentStatus.DECLINED,
                        "SIM-DECLINED",
                        "Rechazado"));
        preparePaymentReference();

        boolean approved = service.processPayment(
                55L,
                form,
                OPERATOR_LOGIN);

        assertThat(approved).isFalse();
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void refundCancelsReservationAndReleasesCapacity() {
        Sale sale = preparePendingSale();
        sale.markPaid(NOW.minusMinutes(5));
        PaymentTransaction originalCharge =
                org.mockito.Mockito.mock(PaymentTransaction.class);
        when(originalCharge.getProvider()).thenReturn(PaymentProvider.AZUL);
        when(paymentRepository
                .findFirstBySale_IdAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                        55L,
                        PaymentTransactionType.CHARGE,
                        PaymentStatus.APPROVED))
                .thenReturn(Optional.of(originalCharge));
        when(gatewayRegistry.resolve(PaymentProvider.AZUL))
                .thenReturn(gateway);
        when(gateway.process(any()))
                .thenReturn(new PaymentResult(
                        PaymentStatus.APPROVED,
                        "SIM-REFUND",
                        "Reembolso aprobado"));
        preparePaymentReference();
        SaleActionForm form = new SaleActionForm();
        form.setReason("Solicitud del cliente");

        service.refund(55L, form, OPERATOR_LOGIN);

        assertThat(sale.getStatus()).isEqualTo(SaleStatus.REFUNDED);
        assertThat(sale.getReservation().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(sale.getRefundReason())
                .isEqualTo("Solicitud del cliente");
        verify(eventPublisher).publishEvent(new SaleRefundedEvent(
                55L,
                "Solicitud del cliente"));
    }

    private Reservation prepareSaleableReservation(int quantity) {
        org.mockito.Mockito.lenient().when(
                userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                OPERATOR_LOGIN,
                OPERATOR_LOGIN)).thenReturn(Optional.of(operator));
        org.mockito.Mockito.lenient().when(event.getId()).thenReturn(8L);
        org.mockito.Mockito.lenient().when(event.getStartAt())
                .thenReturn(NOW.plusDays(2));
        org.mockito.Mockito.lenient().when(
                reservationRepository.findEventIdById(10L))
                .thenReturn(Optional.of(8L));
        org.mockito.Mockito.lenient().when(
                eventRepository.findDetailedByIdForUpdate(8L))
                .thenReturn(Optional.of(event));

        Reservation reservation = new Reservation(
                "RES-ABCDEFGH2345",
                event,
                "María",
                "Pérez",
                "maria@example.com",
                "809-555-0101",
                quantity,
                NOW.plusMinutes(15),
                reservedBy);
        ReflectionTestUtils.setField(reservation, "id", 10L);
        reservation.confirm(NOW.minusMinutes(1));
        org.mockito.Mockito.lenient().when(
                reservationRepository.findDetailedByIdForUpdate(10L))
                .thenReturn(Optional.of(reservation));
        org.mockito.Mockito.lenient().when(
                saleRepository.existsByReservation_Id(10L))
                .thenReturn(false);
        return reservation;
    }

    private Sale preparePendingSale() {
        Reservation reservation = prepareSaleableReservation(1);
        Sale sale = new Sale(
                "SAL-ABCDEFGH2345",
                reservation,
                "DOP",
                operator);
        TicketType general = ticketType(
                31L,
                "General",
                "500.00",
                100);
        sale.addItem(general, 1);
        ReflectionTestUtils.setField(sale, "id", 55L);
        when(saleRepository.findEventIdById(55L))
                .thenReturn(Optional.of(8L));
        when(saleRepository.findReservationIdById(55L))
                .thenReturn(Optional.of(10L));
        when(saleRepository.findDetailedByIdForUpdate(55L))
                .thenReturn(Optional.of(sale));
        return sale;
    }

    private TicketType ticketType(
            Long id,
            String name,
            String price,
            int capacity) {
        TicketType ticketType = new TicketType(
                event,
                TicketTypeCategory.GENERAL,
                name,
                new BigDecimal(price),
                capacity);
        ReflectionTestUtils.setField(ticketType, "id", id);
        return ticketType;
    }

    private SaleForm saleForm(
            Long reservationId,
            Long ticketTypeId,
            int quantity) {
        SaleForm form = new SaleForm();
        form.setReservationId(reservationId);
        form.getItems().getFirst().setTicketTypeId(ticketTypeId);
        form.getItems().getFirst().setQuantity(quantity);
        return form;
    }

    private PaymentForm paymentForm(SimulationOutcome outcome) {
        PaymentForm form = new PaymentForm();
        form.setProvider(PaymentProvider.AZUL);
        form.setSimulationOutcome(outcome);
        return form;
    }

    private void preparePaymentReference() {
        when(referenceGenerator.generatePaymentReference())
                .thenReturn("PAY-ABCDEFGH2345");
        when(paymentRepository.existsByTransactionReference(
                "PAY-ABCDEFGH2345"))
                .thenReturn(false);
    }
}
