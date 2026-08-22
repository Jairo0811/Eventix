package com.jairomatias.eventix.checkout.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.checkout.dto.CustomerCheckoutForm;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutPage;
import com.jairomatias.eventix.checkout.dto.CustomerTicketOption;
import com.jairomatias.eventix.eligibility.service.EventEligibilityService;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.promotion.service.PromotionService;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.event.ReservationConfirmedEvent;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.reservation.service.ReservationProperties;
import com.jairomatias.eventix.reservation.service.ReservationReferenceGenerator;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.sale.service.TransactionReferenceGenerator;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class CustomerCheckoutService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ReservationRepository reservationRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final ReservationReferenceGenerator reservationReferenceGenerator;
    private final TransactionReferenceGenerator transactionReferenceGenerator;
    private final ReservationProperties reservationProperties;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final PromotionService promotionService;
    private final EventEligibilityService eligibilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final String currency;

    public CustomerCheckoutService(
            EventRepository eventRepository,
            TicketTypeRepository ticketTypeRepository,
            ReservationRepository reservationRepository,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            PaymentTransactionRepository paymentRepository,
            UserRepository userRepository,
            ReservationReferenceGenerator reservationReferenceGenerator,
            TransactionReferenceGenerator transactionReferenceGenerator,
            ReservationProperties reservationProperties,
            PaymentGatewayRegistry gatewayRegistry,
            PromotionService promotionService,
            EventEligibilityService eligibilityService,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.currency:DOP}") String currency) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.reservationRepository = reservationRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.reservationReferenceGenerator = reservationReferenceGenerator;
        this.transactionReferenceGenerator = transactionReferenceGenerator;
        this.reservationProperties = reservationProperties;
        this.gatewayRegistry = gatewayRegistry;
        this.promotionService = promotionService;
        this.eligibilityService = eligibilityService;
        this.eventPublisher = eventPublisher;
        this.currency = currency == null ? "DOP" : currency.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public CustomerCheckoutPage getCheckout(Long eventId, String authenticatedLogin) {
        User customer = findCustomer(authenticatedLogin);
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
        LocalDateTime checkedAt = now();
        ensurePurchasable(event, checkedAt);
        eligibilityService.assertEventAccess(event, customer);

        List<CustomerTicketOption> ticketTypes = ticketTypeRepository
                .findAllByEvent_IdAndActiveTrueOrderByNameAsc(eventId)
                .stream()
                .filter(ticketType -> eligibilityService.isTicketVisible(
                        event, customer, ticketType.getId()))
                .map(ticketType -> new CustomerTicketOption(
                        ticketType.getId(),
                        ticketType.getName(),
                        ticketType.getCategory().getDisplayName(),
                        ticketType.getPrice(),
                        Math.max(ticketType.getCapacity()
                                - Math.toIntExact(saleItemRepository.sumAllocatedQuantity(ticketType.getId())), 0)))
                .filter(option -> option.availableQuantity() > 0)
                .toList();

        if (ticketTypes.isEmpty()) {
            throw new BusinessRuleException("Este evento no tiene entradas disponibles en este momento.");
        }
        return new CustomerCheckoutPage(
                event.getId(), event.getTitle(), event.getVenue(), event.getStartAt(),
                event.getCoverImageUrl(), ticketTypes);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public CustomerCheckoutForm getForm(String authenticatedLogin) {
        User customer = findCustomer(authenticatedLogin);
        CustomerCheckoutForm form = new CustomerCheckoutForm();
        form.setFirstName(customer.getFirstName());
        form.setLastName(customer.getLastName());
        form.setEmail(customer.getEmail());
        form.setPhone(customer.getPhone() == null ? "" : customer.getPhone());
        return form;
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public Long purchase(Long eventId, CustomerCheckoutForm form, String authenticatedLogin) {
        if (form == null || form.getTicketTypeId() == null || form.getProvider() == null) {
            throw new BusinessRuleException("Completa los datos de compra.");
        }
        if (form.getQuantity() < 1 || form.getQuantity() > 10) {
            throw new BusinessRuleException("Puedes comprar entre 1 y 10 entradas por operación.");
        }
        if (isBlank(form.getFirstName()) || isBlank(form.getLastName()) || isBlank(form.getPhone())) {
            throw new BusinessRuleException("Completa los datos del asistente.");
        }

        User customer = findCustomer(authenticatedLogin);
        LocalDateTime now = now();
        Event event = eventRepository.findDetailedByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
        ensurePurchasable(event, now);
        reservationRepository.expirePendingForEvent(eventId, now);

        TicketType ticketType = ticketTypeRepository.findDetailedByIdForUpdate(form.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de entrada."));
        if (!ticketType.getEvent().getId().equals(eventId) || !ticketType.isActive()) {
            throw new BusinessRuleException("El tipo de entrada seleccionado no está disponible para este evento.");
        }
        eligibilityService.assertPurchaseAllowed(
                event, customer, ticketType.getId(), form.getQuantity());

        long occupiedSeats = reservationRepository.sumOccupiedSeats(eventId, now);
        if (form.getQuantity() > event.getCapacity() - occupiedSeats) {
            throw new BusinessRuleException("No hay cupos suficientes para completar la compra.");
        }

        long allocated = saleItemRepository.sumAllocatedQuantity(ticketType.getId());
        if (form.getQuantity() > ticketType.getCapacity() - allocated) {
            throw new BusinessRuleException("No hay suficientes entradas disponibles en la categoría seleccionada.");
        }

        String buyerEmail = customer.getEmail().trim().toLowerCase(Locale.ROOT);
        if (reservationRepository.existsActiveDuplicate(eventId, buyerEmail, now, null)) {
            throw new BusinessRuleException("Ya tienes una compra o reservación activa para este evento.");
        }

        Reservation reservation = new Reservation(
                nextReservationReference(),
                event,
                form.getFirstName().trim(),
                form.getLastName().trim(),
                buyerEmail,
                form.getPhone().trim(),
                form.getQuantity(),
                now.plus(reservationProperties.getHoldDuration()),
                customer);
        reservation.confirm(now);
        reservationRepository.save(reservation);
        eventPublisher.publishEvent(new ReservationConfirmedEvent(reservation.getId()));

        Sale sale = new Sale(nextSaleReference(), reservation, currency, customer);
        sale.addItem(ticketType, form.getQuantity());
        Sale savedSale = saleRepository.save(sale);
        promotionService.reserveForSale(form.getCouponCode(), savedSale, now);

        if (savedSale.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            completeFreeSale(savedSale, customer, now);
            return savedSale.getId();
        }

        PaymentCommand command = new PaymentCommand(
                savedSale.getReferenceCode(),
                form.getProvider(),
                PaymentTransactionType.CHARGE,
                savedSale.getTotal(),
                savedSale.getCurrency(),
                SimulationOutcome.APPROVE);
        PaymentResult result = gatewayRegistry.resolve(form.getProvider()).process(command);
        LocalDateTime processedAt = now();
        paymentRepository.save(new PaymentTransaction(
                savedSale,
                nextPaymentReference(),
                form.getProvider(),
                PaymentTransactionType.CHARGE,
                result.status(),
                savedSale.getTotal(),
                savedSale.getCurrency(),
                result.externalReference(),
                result.message(),
                processedAt,
                customer));

        if (result.status() != PaymentStatus.APPROVED) {
            throw new BusinessRuleException("El pago no fue aprobado. Intenta nuevamente.");
        }
        savedSale.markPaid(processedAt);
        promotionService.consumeForSale(savedSale.getId(), processedAt);
        eventPublisher.publishEvent(new SalePaidEvent(savedSale.getId()));
        return savedSale.getId();
    }

    private void completeFreeSale(Sale sale, User customer, LocalDateTime processedAt) {
        sale.markPaid(processedAt);
        paymentRepository.save(new PaymentTransaction(
                sale,
                nextPaymentReference(),
                com.jairomatias.eventix.payment.entity.PaymentProvider.BANK_TRANSFER,
                PaymentTransactionType.CHARGE,
                PaymentStatus.APPROVED,
                BigDecimal.ZERO,
                sale.getCurrency(),
                "NO-COST",
                "Venta sin costo aprobada automáticamente.",
                processedAt,
                customer));
        promotionService.consumeForSale(sale.getId(), processedAt);
        eventPublisher.publishEvent(new SalePaidEvent(sale.getId()));
    }

    private User findCustomer(String login) {
        User user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
        if (user.getRole().getName() != RoleName.USER) {
            throw new BusinessRuleException("Este checkout está disponible únicamente para compradores.");
        }
        return user;
    }

    private void ensurePurchasable(Event event, LocalDateTime now) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("El evento todavía no está disponible para compra.");
        }
        if (!event.getStartAt().isAfter(now)) {
            throw new BusinessRuleException("El evento ya inició o finalizó.");
        }
    }

    private String nextReservationReference() {
        for (int i = 0; i < MAX_REFERENCE_ATTEMPTS; i++) {
            String value = reservationReferenceGenerator.generate();
            if (!reservationRepository.existsByReferenceCode(value)) return value;
        }
        throw new BusinessRuleException("No fue posible generar la referencia de reservación.");
    }

    private String nextSaleReference() {
        for (int i = 0; i < MAX_REFERENCE_ATTEMPTS; i++) {
            String value = transactionReferenceGenerator.generateSaleReference();
            if (!saleRepository.existsByReferenceCode(value)) return value;
        }
        throw new BusinessRuleException("No fue posible generar la referencia de venta.");
    }

    private String nextPaymentReference() {
        for (int i = 0; i < MAX_REFERENCE_ATTEMPTS; i++) {
            String value = transactionReferenceGenerator.generatePaymentReference();
            if (!paymentRepository.existsByTransactionReference(value)) return value;
        }
        throw new BusinessRuleException("No fue posible generar la referencia de pago.");
    }

    private LocalDateTime now() { return LocalDateTime.now(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
