package com.jairomatias.eventix.checkout.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.checkout.dto.CustomerCheckoutForm;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutPage;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutQuote;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutQuoteRequest;
import com.jairomatias.eventix.checkout.dto.CustomerTicketOption;
import com.jairomatias.eventix.eligibility.service.EventEligibilityService;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
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
    private static final Set<PaymentProvider> CUSTOMER_PAYMENT_PROVIDERS = Set.of(
            PaymentProvider.CARDNET,
            PaymentProvider.AZUL,
            PaymentProvider.QIK,
            PaymentProvider.STRIPE,
            PaymentProvider.PAYPAL,
            PaymentProvider.BANK_TRANSFER,
            PaymentProvider.GOOGLE_PAY);

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
        this.currency = currency == null
                ? "DOP"
                : currency.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public CustomerCheckoutPage getCheckout(
            Long eventId,
            String authenticatedLogin) {
        User customer = findCustomer(authenticatedLogin);
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        LocalDateTime checkedAt = now();
        ensurePurchasable(event, checkedAt);
        eligibilityService.assertEventAccess(event, customer);

        List<CustomerTicketOption> ticketTypes = ticketTypeRepository
                .findAllByEvent_IdAndActiveTrueOrderByNameAsc(eventId)
                .stream()
                .filter(ticketType -> eligibilityService.isTicketVisible(
                        event,
                        customer,
                        ticketType.getId()))
                .map(ticketType -> new CustomerTicketOption(
                        ticketType.getId(),
                        ticketType.getName(),
                        ticketType.getCategory().getDisplayName(),
                        ticketType.getPrice(),
                        Math.max(
                                ticketType.getCapacity()
                                        - Math.toIntExact(
                                                saleItemRepository
                                                        .sumAllocatedQuantity(
                                                                ticketType.getId())),
                                0)))
                .filter(option -> option.availableQuantity() > 0)
                .toList();

        if (ticketTypes.isEmpty()) {
            throw new BusinessRuleException(
                    "Este evento no tiene entradas disponibles en este momento.");
        }
        return new CustomerCheckoutPage(
                event.getId(),
                event.getTitle(),
                event.getVenue(),
                event.getStartAt(),
                event.getCoverImageUrl(),
                ticketTypes);
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public CustomerCheckoutQuote quote(
            Long eventId,
            CustomerCheckoutQuoteRequest request,
            String authenticatedLogin) {
        if (request == null || request.ticketTypeId() == null) {
            throw new BusinessRuleException("Selecciona un tipo de entrada.");
        }
        ensureQuantity(request.quantity());

        User customer = findCustomer(authenticatedLogin);
        LocalDateTime quotedAt = now();
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        ensurePurchasable(event, quotedAt);
        eligibilityService.assertEventAccess(event, customer);

        TicketType ticketType = ticketTypeRepository
                .findDetailedById(request.ticketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada."));
        ensureTicketAvailable(
                eventId,
                event,
                customer,
                ticketType,
                request.quantity(),
                quotedAt);

        Reservation quoteReservation = new Reservation(
                "QUOTE",
                event,
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail().trim().toLowerCase(Locale.ROOT),
                customer.getPhone() == null ? "" : customer.getPhone(),
                request.quantity(),
                quotedAt.plus(reservationProperties.getHoldDuration()),
                customer);
        Sale quoteSale = new Sale(
                "QUOTE",
                quoteReservation,
                currency,
                customer);
        quoteSale.addItem(ticketType, request.quantity());
        applyAutomaticEligibilityDiscount(
                event,
                customer,
                ticketType,
                request.couponCode(),
                quoteSale);

        BigDecimal couponDiscount = promotionService.quoteDiscount(
                request.couponCode(),
                quoteSale,
                quotedAt);
        BigDecimal total = quoteSale.getTotal()
                .subtract(couponDiscount)
                .max(BigDecimal.ZERO);
        BigDecimal discount = quoteSale.getSubtotal().subtract(total);
        return new CustomerCheckoutQuote(
                quoteSale.getSubtotal(),
                discount,
                total,
                currency);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public Long purchase(
            Long eventId,
            CustomerCheckoutForm form,
            String authenticatedLogin) {
        if (form == null
                || form.getTicketTypeId() == null
                || form.getProvider() == null) {
            throw new BusinessRuleException("Completa los datos de compra.");
        }
        ensureQuantity(form.getQuantity());
        ensureCustomerProvider(form.getProvider());
        if (isBlank(form.getFirstName())
                || isBlank(form.getLastName())
                || isBlank(form.getPhone())) {
            throw new BusinessRuleException("Completa los datos del asistente.");
        }
        if (form.getProvider().isDigitalWallet()
                && isBlank(form.getWalletToken())) {
            throw new BusinessRuleException(
                    "Google Pay no devolvió un token de pago válido.");
        }

        User customer = findCustomer(authenticatedLogin);
        LocalDateTime currentTime = now();
        Event event = eventRepository.findDetailedByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        ensurePurchasable(event, currentTime);
        reservationRepository.expirePendingForEvent(eventId, currentTime);

        TicketType ticketType = ticketTypeRepository
                .findDetailedByIdForUpdate(form.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada."));
        ensureTicketAvailable(
                eventId,
                event,
                customer,
                ticketType,
                form.getQuantity(),
                currentTime);

        String buyerEmail = customer.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);
        if (reservationRepository.existsActiveDuplicate(
                eventId,
                buyerEmail,
                currentTime,
                null)) {
            throw new BusinessRuleException(
                    "Ya tienes una compra o reservación activa para este evento.");
        }

        Reservation reservation = new Reservation(
                nextReservationReference(),
                event,
                form.getFirstName().trim(),
                form.getLastName().trim(),
                buyerEmail,
                form.getPhone().trim(),
                form.getQuantity(),
                currentTime.plus(reservationProperties.getHoldDuration()),
                customer);
        reservation.confirm(currentTime);
        reservationRepository.save(reservation);
        eventPublisher.publishEvent(new ReservationConfirmedEvent(
                reservation.getId()));

        Sale sale = new Sale(
                nextSaleReference(),
                reservation,
                currency,
                customer);
        sale.addItem(ticketType, form.getQuantity());
        applyAutomaticEligibilityDiscount(
                event,
                customer,
                ticketType,
                form.getCouponCode(),
                sale);

        Sale savedSale = saleRepository.save(sale);
        promotionService.reserveForSale(
                form.getCouponCode(),
                savedSale,
                currentTime);

        if (savedSale.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            completeFreeSale(savedSale, customer, currentTime);
            return savedSale.getId();
        }

        PaymentCommand command = new PaymentCommand(
                savedSale.getReferenceCode(),
                form.getProvider(),
                PaymentTransactionType.CHARGE,
                savedSale.getTotal(),
                savedSale.getCurrency(),
                SimulationOutcome.APPROVE,
                form.getProvider().isDigitalWallet()
                        ? form.getWalletToken()
                        : null,
                null);
        PaymentResult result = gatewayRegistry
                .resolve(form.getProvider())
                .process(command);
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
            throw new BusinessRuleException(
                    "El pago no fue aprobado. Intenta nuevamente.");
        }
        savedSale.markPaid(processedAt);
        promotionService.consumeForSale(savedSale.getId(), processedAt);
        eventPublisher.publishEvent(new SalePaidEvent(savedSale.getId()));
        return savedSale.getId();
    }

    private void ensureTicketAvailable(
            Long eventId,
            Event event,
            User customer,
            TicketType ticketType,
            int quantity,
            LocalDateTime at) {
        if (!ticketType.getEvent().getId().equals(eventId)
                || !ticketType.isActive()) {
            throw new BusinessRuleException(
                    "El tipo de entrada seleccionado no está disponible para este evento.");
        }
        eligibilityService.assertPurchaseAllowed(
                event,
                customer,
                ticketType.getId(),
                quantity);

        long occupiedSeats = reservationRepository.sumOccupiedSeats(
                eventId,
                at);
        if (quantity > event.getCapacity() - occupiedSeats) {
            throw new BusinessRuleException(
                    "No hay cupos suficientes para completar la compra.");
        }

        long allocated = saleItemRepository.sumAllocatedQuantity(
                ticketType.getId());
        if (quantity > ticketType.getCapacity() - allocated) {
            throw new BusinessRuleException(
                    "No hay suficientes entradas disponibles en la categoría seleccionada.");
        }
    }

    private void applyAutomaticEligibilityDiscount(
            Event event,
            User customer,
            TicketType ticketType,
            String couponCode,
            Sale sale) {
        eligibilityService.resolveMonetaryDiscount(
                        event,
                        customer,
                        ticketType.getId(),
                        sale.getSubtotal())
                .ifPresent(decision -> {
                    if (!isBlank(couponCode)) {
                        throw new BusinessRuleException(
                                "Los beneficios monetarios de elegibilidad no se combinan con cupones todavía. "
                                        + "Retira el cupón para usar el beneficio automático.");
                    }
                    sale.applyEligibilityDiscount(
                            decision.benefitId(),
                            decision.benefitType(),
                            decision.configuredValue(),
                            decision.discountAmount());
                });
    }

    private void ensureQuantity(int quantity) {
        if (quantity < 1 || quantity > 10) {
            throw new BusinessRuleException(
                    "Puedes comprar entre 1 y 10 entradas por operación.");
        }
    }

    private void ensureCustomerProvider(PaymentProvider provider) {
        if (!CUSTOMER_PAYMENT_PROVIDERS.contains(provider)) {
            throw new BusinessRuleException(
                    "El método de pago seleccionado no está disponible en el checkout.");
        }
    }

    private void completeFreeSale(
            Sale sale,
            User customer,
            LocalDateTime processedAt) {
        sale.markPaid(processedAt);
        paymentRepository.save(new PaymentTransaction(
                sale,
                nextPaymentReference(),
                PaymentProvider.BANK_TRANSFER,
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
        User user = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
        if (user.getRole().getName() != RoleName.USER) {
            throw new BusinessRuleException(
                    "Este checkout está disponible únicamente para compradores.");
        }
        return user;
    }

    private void ensurePurchasable(Event event, LocalDateTime currentTime) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "El evento todavía no está disponible para compra.");
        }
        if (!event.getStartAt().isAfter(currentTime)) {
            throw new BusinessRuleException(
                    "El evento ya inició o finalizó.");
        }
    }

    private String nextReservationReference() {
        for (int attempt = 0;
                attempt < MAX_REFERENCE_ATTEMPTS;
                attempt++) {
            String value = reservationReferenceGenerator.generate();
            if (!reservationRepository.existsByReferenceCode(value)) {
                return value;
            }
        }
        throw new BusinessRuleException(
                "No fue posible generar la referencia de reservación.");
    }

    private String nextSaleReference() {
        for (int attempt = 0;
                attempt < MAX_REFERENCE_ATTEMPTS;
                attempt++) {
            String value = transactionReferenceGenerator.generateSaleReference();
            if (!saleRepository.existsByReferenceCode(value)) {
                return value;
            }
        }
        throw new BusinessRuleException(
                "No fue posible generar la referencia de venta.");
    }

    private String nextPaymentReference() {
        for (int attempt = 0;
                attempt < MAX_REFERENCE_ATTEMPTS;
                attempt++) {
            String value = transactionReferenceGenerator.generatePaymentReference();
            if (!paymentRepository.existsByTransactionReference(value)) {
                return value;
            }
        }
        throw new BusinessRuleException(
                "No fue posible generar una referencia de pago única.");
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
