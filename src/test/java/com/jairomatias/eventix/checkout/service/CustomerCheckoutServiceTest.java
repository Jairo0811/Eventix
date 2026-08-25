package com.jairomatias.eventix.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.jairomatias.eventix.checkout.dto.CustomerCheckoutForm;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutPage;
import com.jairomatias.eventix.eligibility.dto.EligibilityDiscountDecision;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.service.EventEligibilityService;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.promotion.service.PromotionService;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.reservation.service.ReservationProperties;
import com.jairomatias.eventix.reservation.service.ReservationReferenceGenerator;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.sale.service.TransactionReferenceGenerator;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomerCheckoutServiceTest {

    private static final String CUSTOMER_LOGIN = "buyer@eventix.local";

    @Mock private EventRepository eventRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private SaleItemRepository saleItemRepository;
    @Mock private PaymentTransactionRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReservationReferenceGenerator reservationReferenceGenerator;
    @Mock private TransactionReferenceGenerator transactionReferenceGenerator;
    @Mock private ReservationProperties reservationProperties;
    @Mock private PaymentGatewayRegistry gatewayRegistry;
    @Mock private PromotionService promotionService;
    @Mock private EventEligibilityService eligibilityService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private User customer;
    @Mock private Role customerRole;
    @Mock private Event event;
    @Mock private TicketType ticketType;

    private CustomerCheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CustomerCheckoutService(
                eventRepository,
                ticketTypeRepository,
                reservationRepository,
                saleRepository,
                saleItemRepository,
                paymentRepository,
                userRepository,
                reservationReferenceGenerator,
                transactionReferenceGenerator,
                reservationProperties,
                gatewayRegistry,
                promotionService,
                eligibilityService,
                eventPublisher,
                "DOP");
    }

    @Test
    void preloadsBuyerDataFromAuthenticatedAccount() {
        prepareCustomer();
        when(customer.getFirstName()).thenReturn("Ana");
        when(customer.getLastName()).thenReturn("Pérez");
        when(customer.getEmail()).thenReturn(CUSTOMER_LOGIN);
        when(customer.getPhone()).thenReturn("8095550000");

        CustomerCheckoutForm form = service.getForm(CUSTOMER_LOGIN);

        assertThat(form.getFirstName()).isEqualTo("Ana");
        assertThat(form.getLastName()).isEqualTo("Pérez");
        assertThat(form.getEmail()).isEqualTo(CUSTOMER_LOGIN);
        assertThat(form.getPhone()).isEqualTo("8095550000");
    }

    @Test
    void returnsOnlyTicketTypesAuthorizedForCustomer() {
        prepareCustomer();
        preparePublishedEvent();
        when(eventRepository.findDetailedById(10L))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository
                .findAllByEvent_IdAndActiveTrueOrderByNameAsc(10L))
                .thenReturn(List.of(ticketType));
        when(eligibilityService.isTicketVisible(event, customer, 31L))
                .thenReturn(true);
        when(ticketType.getId()).thenReturn(31L);
        when(ticketType.getName()).thenReturn("General");
        when(ticketType.getCategory()).thenReturn(TicketTypeCategory.GENERAL);
        when(ticketType.getPrice()).thenReturn(new BigDecimal("500.00"));
        when(ticketType.getCapacity()).thenReturn(50);
        when(saleItemRepository.sumAllocatedQuantity(31L)).thenReturn(7L);

        CustomerCheckoutPage checkout =
                service.getCheckout(10L, CUSTOMER_LOGIN);

        assertThat(checkout.ticketTypes()).singleElement()
                .satisfies(option -> {
                    assertThat(option.id()).isEqualTo(31L);
                    assertThat(option.availableQuantity()).isEqualTo(43);
                });
    }

    @Test
    void completesFreeEligibilityPurchaseWithoutCallingGateway() {
        prepareCustomer();
        when(customer.getEmail()).thenReturn(CUSTOMER_LOGIN);
        preparePublishedEvent();
        prepareTicketType();
        when(eventRepository.findDetailedByIdForUpdate(10L))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.findDetailedByIdForUpdate(31L))
                .thenReturn(Optional.of(ticketType));
        when(reservationRepository.sumOccupiedSeats(any(), any()))
                .thenReturn(0L);
        when(saleItemRepository.sumAllocatedQuantity(31L)).thenReturn(0L);
        when(reservationRepository.existsActiveDuplicate(
                any(), any(), any(), any())).thenReturn(false);
        when(reservationReferenceGenerator.generate())
                .thenReturn("RSV-ABCDEFGH2345");
        when(reservationRepository.existsByReferenceCode("RSV-ABCDEFGH2345"))
                .thenReturn(false);
        when(transactionReferenceGenerator.generateSaleReference())
                .thenReturn("SAL-ABCDEFGH2345");
        when(saleRepository.existsByReferenceCode("SAL-ABCDEFGH2345"))
                .thenReturn(false);
        when(transactionReferenceGenerator.generatePaymentReference())
                .thenReturn("PAY-ABCDEFGH2345");
        when(paymentRepository.existsByTransactionReference("PAY-ABCDEFGH2345"))
                .thenReturn(false);
        when(reservationProperties.getHoldDuration())
                .thenReturn(Duration.ofMinutes(15));
        when(eligibilityService.resolveMonetaryDiscount(
                event,
                customer,
                31L,
                new BigDecimal("1000.00")))
                .thenReturn(Optional.of(new EligibilityDiscountDecision(
                        91L,
                        EligibilityBenefitType.FREE_ENTRY,
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"))));
        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale sale = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sale, "id", 55L);
                    return sale;
                });

        Long saleId = service.purchase(
                10L,
                validForm(2),
                CUSTOMER_LOGIN);

        assertThat(saleId).isEqualTo(55L);
        verifyNoInteractions(gatewayRegistry);
        org.mockito.Mockito.verify(paymentRepository)
                .save(any(PaymentTransaction.class));
    }

    @Test
    void rejectsQuantityAboveCheckoutLimitBeforePersistence() {
        CustomerCheckoutForm form = validForm(11);

        assertThatThrownBy(() -> service.purchase(
                10L,
                form,
                CUSTOMER_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entre 1 y 10");

        verifyNoInteractions(userRepository, eventRepository, saleRepository);
    }

    private void prepareCustomer() {
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                CUSTOMER_LOGIN,
                CUSTOMER_LOGIN)).thenReturn(Optional.of(customer));
        when(customer.getRole()).thenReturn(customerRole);
        when(customerRole.getName()).thenReturn(RoleName.USER);
    }

    private void preparePublishedEvent() {
        when(event.getId()).thenReturn(10L);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getStartAt()).thenReturn(
                LocalDateTime.of(2099, 1, 1, 20, 0));
        org.mockito.Mockito.lenient().when(event.getCapacity()).thenReturn(100);
        org.mockito.Mockito.lenient().when(event.getTitle())
                .thenReturn("Evento de prueba");
        org.mockito.Mockito.lenient().when(event.getVenue())
                .thenReturn("Santo Domingo");
    }

    private void prepareTicketType() {
        when(ticketType.getId()).thenReturn(31L);
        when(ticketType.getEvent()).thenReturn(event);
        when(ticketType.isActive()).thenReturn(true);
        when(ticketType.getCapacity()).thenReturn(100);
        when(ticketType.getName()).thenReturn("General");
        when(ticketType.getPrice()).thenReturn(new BigDecimal("500.00"));
    }

    private CustomerCheckoutForm validForm(int quantity) {
        CustomerCheckoutForm form = new CustomerCheckoutForm();
        form.setTicketTypeId(31L);
        form.setQuantity(quantity);
        form.setFirstName("Ana");
        form.setLastName("Pérez");
        form.setEmail(CUSTOMER_LOGIN);
        form.setPhone("8095550000");
        form.setProvider(PaymentProvider.CARDNET);
        return form;
    }
}
