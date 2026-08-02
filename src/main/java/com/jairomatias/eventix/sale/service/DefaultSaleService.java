package com.jairomatias.eventix.sale.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.payment.dto.PaymentForm;
import com.jairomatias.eventix.payment.dto.PaymentTransactionView;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.dto.ReservationSaleOption;
import com.jairomatias.eventix.sale.dto.SaleActionForm;
import com.jairomatias.eventix.sale.dto.SaleDetailsView;
import com.jairomatias.eventix.sale.dto.SaleForm;
import com.jairomatias.eventix.sale.dto.SaleItemView;
import com.jairomatias.eventix.sale.dto.SaleLineForm;
import com.jairomatias.eventix.sale.dto.SaleListItem;
import com.jairomatias.eventix.sale.dto.SalesSummary;
import com.jairomatias.eventix.sale.dto.TicketTypeOption;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleItem;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultSaleService implements SaleService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final TransactionReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final String currency;
    private final Clock clock;

    @Autowired
    public DefaultSaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            TicketTypeRepository ticketTypeRepository,
            PaymentTransactionRepository paymentRepository,
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            PaymentGatewayRegistry gatewayRegistry,
            TransactionReferenceGenerator referenceGenerator,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.currency:DOP}") String currency) {
        this(
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
                currency,
                Clock.systemDefaultZone());
    }

    DefaultSaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            TicketTypeRepository ticketTypeRepository,
            PaymentTransactionRepository paymentRepository,
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            PaymentGatewayRegistry gatewayRegistry,
            TransactionReferenceGenerator referenceGenerator,
            ApplicationEventPublisher eventPublisher,
            String currency,
            Clock clock) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.gatewayRegistry = gatewayRegistry;
        this.referenceGenerator = referenceGenerator;
        this.eventPublisher = eventPublisher;
        this.currency = normalizeCurrency(currency);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public Page<SaleListItem> findAll(
            String term,
            SaleStatus status,
            Long eventId,
            String authenticatedLogin,
            Pageable pageable) {
        User actor = findActor(authenticatedLogin);
        Long organizerId = actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;

        return saleRepository.search(
                        normalizeSearchTerm(term),
                        status,
                        eventId,
                        organizerId,
                        pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public SaleDetailsView findById(
            Long id,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Sale sale = findSale(id);
        ensureCanView(sale, actor);
        return toDetailsView(sale);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public SalesSummary getSummary(String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Long organizerId = actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;
        BigDecimal gross = safeAmount(saleRepository.sumTotalByStatus(
                SaleStatus.PAID,
                organizerId));
        BigDecimal refunded = safeAmount(saleRepository.sumTotalByStatus(
                SaleStatus.REFUNDED,
                organizerId));

        return new SalesSummary(
                saleRepository.countByOrganizer(organizerId),
                saleRepository.countByStatusAndOrganizer(
                        SaleStatus.PENDING,
                        organizerId),
                saleRepository.countByStatusAndOrganizer(
                        SaleStatus.PAID,
                        organizerId),
                saleRepository.countByStatusAndOrganizer(
                        SaleStatus.REFUNDED,
                        organizerId),
                saleRepository.countByStatusAndOrganizer(
                        SaleStatus.CANCELLED,
                        organizerId),
                gross,
                refunded,
                gross.subtract(refunded));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public List<EventReservationOption> findVisibleEvents(
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        List<Event> events = actor.getRole().getName() == RoleName.ORGANIZER
                ? eventRepository.findAllByOrganizer_IdOrderByStartAtDesc(
                        actor.getId())
                : eventRepository.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "startAt"));
        return events.stream()
                .map(event -> new EventReservationOption(
                        event.getId(),
                        event.getTitle(),
                        event.getStartAt(),
                        event.getCapacity()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public List<ReservationSaleOption> findSaleableReservations() {
        return reservationRepository.findConfirmedWithoutSale(now())
                .stream()
                .map(reservation -> new ReservationSaleOption(
                        reservation.getId(),
                        reservation.getReferenceCode(),
                        reservation.getEvent().getId(),
                        reservation.getEvent().getTitle(),
                        reservation.getAttendeeFullName(),
                        reservation.getQuantity()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public SaleForm getCreateForm(
            Long reservationId,
            String authenticatedLogin) {
        findActor(authenticatedLogin);
        SaleForm form = new SaleForm();
        form.setReservationId(reservationId);
        if (reservationId != null) {
            ensureSaleable(findReservation(reservationId));
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public List<TicketTypeOption> findTicketTypeOptions(
            Long reservationId,
            String authenticatedLogin) {
        findActor(authenticatedLogin);
        if (reservationId == null) {
            return List.of();
        }
        Reservation reservation = findReservation(reservationId);
        ensureSaleable(reservation);

        return ticketTypeRepository
                .findAllByEvent_IdAndActiveTrueOrderByNameAsc(
                        reservation.getEvent().getId())
                .stream()
                .map(ticketType -> {
                    int allocated = Math.toIntExact(
                            saleItemRepository.sumAllocatedQuantity(
                                    ticketType.getId()));
                    return new TicketTypeOption(
                            ticketType.getId(),
                            ticketType.getName(),
                            ticketType.getPrice(),
                            Math.max(ticketType.getCapacity() - allocated, 0));
                })
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public Long create(
            SaleForm form,
            String authenticatedLogin) {
        validateForm(form);
        User actor = findActor(authenticatedLogin);
        Long eventId = findReservationEventId(form.getReservationId());
        Event event = findEventForUpdate(eventId);
        Reservation reservation = findReservationForUpdate(
                form.getReservationId());
        ensureSaleable(reservation);

        Map<Long, Integer> requestedItems = consolidateItems(form.getItems());
        int requestedQuantity = requestedItems.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        if (requestedQuantity != reservation.getQuantity()) {
            throw new BusinessRuleException(
                    "La suma de entradas debe coincidir con los "
                    + reservation.getQuantity()
                    + " cupos de la reservación.");
        }

        Sale sale = new Sale(
                nextSaleReference(),
                reservation,
                currency,
                actor);
        for (Map.Entry<Long, Integer> entry : requestedItems.entrySet()) {
            TicketType ticketType = findTicketTypeForUpdate(entry.getKey());
            ensureTicketTypeAvailable(
                    ticketType,
                    event,
                    entry.getValue());
            sale.addItem(ticketType, entry.getValue());
        }

        Sale saved = saleRepository.save(sale);
        if (saved.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            LocalDateTime processedAt = now();
            saved.markPaid(processedAt);
            paymentRepository.save(new PaymentTransaction(
                    saved,
                    nextPaymentReference(),
                    PaymentProvider.BANK_TRANSFER,
                    PaymentTransactionType.CHARGE,
                    PaymentStatus.APPROVED,
                    BigDecimal.ZERO,
                    saved.getCurrency(),
                    "NO-COST",
                    "Venta sin costo aprobada automáticamente.",
                    processedAt,
                    actor));
            eventPublisher.publishEvent(new SalePaidEvent(saved.getId()));
        }
        return saved.getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public boolean processPayment(
            Long id,
            PaymentForm form,
            String authenticatedLogin) {
        validatePaymentForm(form);
        User actor = findActor(authenticatedLogin);
        Sale sale = lockSaleAggregate(id);
        ensurePendingAndCurrent(sale);

        PaymentCommand command = new PaymentCommand(
                sale.getReferenceCode(),
                form.getProvider(),
                PaymentTransactionType.CHARGE,
                sale.getTotal(),
                sale.getCurrency(),
                form.getSimulationOutcome());
        PaymentResult result = gatewayRegistry
                .resolve(form.getProvider())
                .process(command);
        LocalDateTime processedAt = now();

        paymentRepository.save(new PaymentTransaction(
                sale,
                nextPaymentReference(),
                form.getProvider(),
                PaymentTransactionType.CHARGE,
                result.status(),
                sale.getTotal(),
                sale.getCurrency(),
                result.externalReference(),
                result.message(),
                processedAt,
                actor));

        if (result.status() == PaymentStatus.APPROVED) {
            sale.markPaid(processedAt);
            eventPublisher.publishEvent(new SalePaidEvent(sale.getId()));
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public void refund(
            Long id,
            SaleActionForm form,
            String authenticatedLogin) {
        String reason = validatedReason(form);
        User actor = findActor(authenticatedLogin);
        Sale sale = lockSaleAggregate(id);
        if (sale.getStatus() != SaleStatus.PAID) {
            throw new BusinessRuleException(
                    "Solo se pueden reembolsar ventas pagadas.");
        }
        ensureEventHasNotStarted(sale.getEvent());

        PaymentTransaction originalCharge = paymentRepository
                .findFirstBySale_IdAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                        sale.getId(),
                        PaymentTransactionType.CHARGE,
                        PaymentStatus.APPROVED)
                .orElseThrow(() -> new BusinessRuleException(
                        "La venta no tiene un cobro aprobado para reembolsar."));
        PaymentCommand command = new PaymentCommand(
                sale.getReferenceCode(),
                originalCharge.getProvider(),
                PaymentTransactionType.REFUND,
                sale.getTotal(),
                sale.getCurrency(),
                SimulationOutcome.APPROVE);
        PaymentResult result = gatewayRegistry
                .resolve(originalCharge.getProvider())
                .process(command);
        LocalDateTime processedAt = now();

        paymentRepository.save(new PaymentTransaction(
                sale,
                nextPaymentReference(),
                originalCharge.getProvider(),
                PaymentTransactionType.REFUND,
                result.status(),
                sale.getTotal(),
                sale.getCurrency(),
                result.externalReference(),
                truncate(reason + " — " + result.message(), 300),
                processedAt,
                actor));

        if (result.status() != PaymentStatus.APPROVED) {
            throw new BusinessRuleException(
                    "La pasarela no aprobó el reembolso.");
        }
        sale.markRefunded(reason, processedAt);
        cancelLinkedReservation(sale, reason, processedAt);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public void cancel(
            Long id,
            SaleActionForm form,
            String authenticatedLogin) {
        String reason = validatedReason(form);
        findActor(authenticatedLogin);
        Sale sale = lockSaleAggregate(id);
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new BusinessRuleException(
                    "Solo se pueden cancelar ventas pendientes.");
        }
        ensureEventHasNotStarted(sale.getEvent());
        LocalDateTime cancelledAt = now();
        sale.cancel(reason, cancelledAt);
        cancelLinkedReservation(sale, reason, cancelledAt);
    }

    private Sale lockSaleAggregate(Long saleId) {
        Long eventId = saleRepository.findEventIdById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));
        Long reservationId = saleRepository.findReservationIdById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));
        findEventForUpdate(eventId);
        findReservationForUpdate(reservationId);
        return findSaleForUpdate(saleId);
    }

    private void ensureTicketTypeAvailable(
            TicketType ticketType,
            Event event,
            int quantity) {
        if (!ticketType.getEvent().getId().equals(event.getId())) {
            throw new BusinessRuleException(
                    "Todos los tipos de entrada deben pertenecer al evento reservado.");
        }
        if (!ticketType.isActive()) {
            throw new BusinessRuleException(
                    "El tipo de entrada " + ticketType.getName() + " está inactivo.");
        }
        long allocated = saleItemRepository.sumAllocatedQuantity(
                ticketType.getId());
        long available = ticketType.getCapacity() - allocated;
        if (quantity > available) {
            throw new BusinessRuleException(
                    "El tipo "
                    + ticketType.getName()
                    + " solo dispone de "
                    + Math.max(available, 0)
                    + " entradas.");
        }
    }

    private void ensureSaleable(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "La venta requiere una reservación confirmada.");
        }
        ensureEventHasNotStarted(reservation.getEvent());
        if (saleRepository.existsByReservation_Id(reservation.getId())) {
            throw new BusinessRuleException(
                    "La reservación ya tiene una venta asociada.");
        }
    }

    private void ensurePendingAndCurrent(Sale sale) {
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new BusinessRuleException(
                    "Solo se pueden cobrar ventas pendientes.");
        }
        ensureEventHasNotStarted(sale.getEvent());
    }

    private void ensureEventHasNotStarted(Event event) {
        if (!event.getStartAt().isAfter(now())) {
            throw new BusinessRuleException(
                    "No se puede operar la venta después de iniciar el evento.");
        }
    }

    private void cancelLinkedReservation(
            Sale sale,
            String reason,
            LocalDateTime at) {
        Reservation reservation = sale.getReservation();
        if (reservation.getStatus().isActive()) {
            reservation.cancel(
                    truncate(
                            "Venta " + sale.getReferenceCode() + ": " + reason,
                            500),
                    at);
        }
    }

    private void ensureCanView(Sale sale, User actor) {
        RoleName role = actor.getRole().getName();
        if (role == RoleName.ADMINISTRATOR || role == RoleName.OPERATOR) {
            return;
        }
        if (role == RoleName.ORGANIZER
                && sale.getEvent().getOrganizer().getId()
                        .equals(actor.getId())) {
            return;
        }
        throw new BusinessRuleException(
                "No tienes permiso para consultar esta venta.");
    }

    private Map<Long, Integer> consolidateItems(List<SaleLineForm> items) {
        Map<Long, Integer> consolidated = new TreeMap<>();
        if (items != null) {
            for (SaleLineForm item : items) {
                if (item == null
                        || item.getTicketTypeId() == null
                        || item.getQuantity() == 0) {
                    continue;
                }
                if (item.getQuantity() < 0) {
                    throw new BusinessRuleException(
                            "La cantidad de entradas no puede ser negativa.");
                }
                consolidated.merge(
                        item.getTicketTypeId(),
                        item.getQuantity(),
                        Integer::sum);
            }
        }
        if (consolidated.isEmpty()) {
            throw new BusinessRuleException(
                    "Agrega al menos un tipo de entrada a la venta.");
        }
        return new LinkedHashMap<>(consolidated);
    }

    private void validateForm(SaleForm form) {
        if (form == null || form.getReservationId() == null) {
            throw new BusinessRuleException(
                    "Selecciona una reservación confirmada.");
        }
    }

    private void validatePaymentForm(PaymentForm form) {
        if (form == null || form.getProvider() == null) {
            throw new BusinessRuleException(
                    "Selecciona el proveedor de pago.");
        }
        if (form.getSimulationOutcome() == null) {
            throw new BusinessRuleException(
                    "Selecciona el resultado de la simulación.");
        }
    }

    private String validatedReason(SaleActionForm form) {
        if (form == null
                || form.getReason() == null
                || form.getReason().isBlank()) {
            throw new BusinessRuleException(
                    "Indica el motivo de la operación.");
        }
        String reason = form.getReason().trim();
        if (reason.length() > 500) {
            throw new BusinessRuleException(
                    "El motivo no puede exceder 500 caracteres.");
        }
        return reason;
    }

    private SaleListItem toListItem(Sale sale) {
        return new SaleListItem(
                sale.getId(),
                sale.getReferenceCode(),
                sale.getEvent().getId(),
                sale.getEvent().getTitle(),
                sale.getReservation().getReferenceCode(),
                sale.getBuyerName(),
                sale.getBuyerEmail(),
                sale.getStatus(),
                sale.getTotal(),
                sale.getCurrency(),
                sale.getCreatedAt());
    }

    private SaleDetailsView toDetailsView(Sale sale) {
        List<SaleItemView> items = sale.getItems().stream()
                .map(this::toItemView)
                .toList();
        List<PaymentTransactionView> payments = paymentRepository
                .findAllBySale_IdOrderByProcessedAtDesc(sale.getId())
                .stream()
                .map(this::toPaymentView)
                .toList();
        return new SaleDetailsView(
                sale.getId(),
                sale.getReferenceCode(),
                sale.getReservation().getId(),
                sale.getReservation().getReferenceCode(),
                sale.getEvent().getId(),
                sale.getEvent().getTitle(),
                sale.getEvent().getStartAt(),
                sale.getBuyerName(),
                sale.getBuyerEmail(),
                sale.getBuyerPhone(),
                sale.getStatus(),
                sale.getCurrency(),
                sale.getSubtotal(),
                sale.getDiscountTotal(),
                sale.getTotal(),
                sale.getPaidAt(),
                sale.getRefundedAt(),
                sale.getRefundReason(),
                sale.getCancelledAt(),
                sale.getCancellationReason(),
                sale.getSoldBy().getFullName(),
                sale.getCreatedAt(),
                items,
                payments);
    }

    private SaleItemView toItemView(SaleItem item) {
        return new SaleItemView(
                item.getId(),
                item.getTicketType().getId(),
                item.getTicketTypeName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal());
    }

    private PaymentTransactionView toPaymentView(
            PaymentTransaction payment) {
        return new PaymentTransactionView(
                payment.getId(),
                payment.getTransactionReference(),
                payment.getProvider(),
                payment.getTransactionType(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getExternalReference(),
                payment.getResponseMessage(),
                payment.getProcessedAt(),
                payment.getProcessedBy().getFullName());
    }

    private String nextSaleReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String reference = referenceGenerator.generateSaleReference();
            if (!saleRepository.existsByReferenceCode(reference)) {
                return reference;
            }
        }
        throw new BusinessRuleException(
                "No se pudo generar una referencia única para la venta.");
    }

    private String nextPaymentReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String reference = referenceGenerator.generatePaymentReference();
            if (!paymentRepository.existsByTransactionReference(reference)) {
                return reference;
            }
        }
        throw new BusinessRuleException(
                "No se pudo generar una referencia única para el pago.");
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reservación solicitada."));
    }

    private Reservation findReservationForUpdate(Long id) {
        return reservationRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reservación solicitada."));
    }

    private Long findReservationEventId(Long reservationId) {
        return reservationRepository.findEventIdById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reservación solicitada."));
    }

    private Event findEventForUpdate(Long id) {
        return eventRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
    }

    private TicketType findTicketTypeForUpdate(Long id) {
        return ticketTypeRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada solicitado."));
    }

    private Sale findSale(Long id) {
        return saleRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));
    }

    private Sale findSaleForUpdate(Long id) {
        return saleRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));
    }

    private String normalizeSearchTerm(String term) {
        return term == null ? "" : term.trim();
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String truncate(String value, int maximumLength) {
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }

    private static String normalizeCurrency(String value) {
        String normalized = value == null
                ? "DOP"
                : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new IllegalArgumentException(
                    "La moneda debe usar un código ISO de tres caracteres.");
        }
        return normalized;
    }
}
