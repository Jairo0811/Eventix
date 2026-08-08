package com.jairomatias.eventix.ticket.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.dto.AccessDashboardSummary;
import com.jairomatias.eventix.ticket.dto.ScanAttemptListItem;
import com.jairomatias.eventix.ticket.dto.ScanForm;
import com.jairomatias.eventix.ticket.dto.ScanResultView;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.ScanOutcome;
import com.jairomatias.eventix.ticket.entity.TicketScanAttempt;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.event.TicketPassChangedEvent;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.repository.TicketScanAttemptRepository;
import com.jairomatias.eventix.ticket.security.ParsedTicketToken;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultAccessControlService implements AccessControlService {

    private final DigitalTicketRepository ticketRepository;
    private final TicketScanAttemptRepository attemptRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketCryptographyService cryptographyService;
    private final TicketingProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public DefaultAccessControlService(
            DigitalTicketRepository ticketRepository,
            TicketScanAttemptRepository attemptRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            TicketCryptographyService cryptographyService,
            TicketingProperties properties,
            ApplicationEventPublisher eventPublisher) {
        this(
                ticketRepository,
                attemptRepository,
                eventRepository,
                userRepository,
                cryptographyService,
                properties,
                eventPublisher,
                Clock.systemDefaultZone());
    }

    DefaultAccessControlService(
            DigitalTicketRepository ticketRepository,
            TicketScanAttemptRepository attemptRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            TicketCryptographyService cryptographyService,
            TicketingProperties properties,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.attemptRepository = attemptRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.cryptographyService = cryptographyService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ACCESS_STAFF')")
    public ScanResultView scan(
            ScanForm form,
            String authenticatedLogin,
            String ipAddress) {
        User actor = findActor(authenticatedLogin);
        LocalDateTime occurredAt = LocalDateTime.now(clock).withNano(0);
        String rawCodeHash = cryptographyService.sha256(form.getToken());
        String device = normalizeDevice(form.getDeviceIdentifier());
        String clientIp = normalizeIp(ipAddress);

        Optional<ParsedTicketToken> parsed =
                cryptographyService.parseQrPayload(form.getToken());
        if (parsed.isEmpty()) {
            return record(
                    null,
                    rawCodeHash,
                    ScanOutcome.COUNTERFEIT,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    false,
                    "Formato de QR no reconocido.");
        }

        Optional<DigitalTicket> stored = ticketRepository
                .findByUniqueCodeForUpdate(parsed.get().uniqueCode());
        if (stored.isEmpty()) {
            return record(
                    null,
                    rawCodeHash,
                    ScanOutcome.COUNTERFEIT,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    false,
                    "Código de boleta inexistente.");
        }

        DigitalTicket ticket = stored.get();
        if (!cryptographyService.verify(ticket, parsed.get())) {
            return record(
                    ticket,
                    rawCodeHash,
                    ScanOutcome.COUNTERFEIT,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    false,
                    "La firma digital o el código antifraude no coincide.");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return record(
                    ticket,
                    rawCodeHash,
                    ScanOutcome.CANCELLED,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    false,
                    ticket.getCancellationReason());
        }

        if (ticket.getEvent().getStatus() == EventStatus.CANCELLED) {
            ticket.cancel("El evento fue cancelado.", occurredAt);
            eventPublisher.publishEvent(
                    new TicketPassChangedEvent(ticket.getId()));
            return record(
                    ticket,
                    rawCodeHash,
                    ScanOutcome.CANCELLED,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    false,
                    "El evento fue cancelado.");
        }

        if (ticket.getStatus() == TicketStatus.EXPIRED
                || ticket.getEvent().getStatus() == EventStatus.FINISHED
                || ticket.getEvent().getEndAt().isBefore(occurredAt)) {
            if (ticket.getStatus() == TicketStatus.ACTIVE) {
                ticket.expire(occurredAt);
                eventPublisher.publishEvent(
                        new TicketPassChangedEvent(ticket.getId()));
            }
            return record(
                    ticket,
                    rawCodeHash,
                    ScanOutcome.EXPIRED,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    false,
                    "El evento finalizó o la boleta venció.");
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            boolean reentry = form.isReentry()
                    && properties.isAllowReentry();
            return record(
                    ticket,
                    rawCodeHash,
                    reentry ? ScanOutcome.REENTRY : ScanOutcome.DUPLICATE,
                    occurredAt,
                    actor,
                    device,
                    clientIp,
                    false,
                    !reentry,
                    reentry
                            ? "Reingreso autorizado por el personal de acceso."
                            : "La boleta ya registró su primer acceso.");
        }

        ticket.markUsed(occurredAt);
        eventPublisher.publishEvent(
                new TicketPassChangedEvent(ticket.getId()));
        return record(
                ticket,
                rawCodeHash,
                ScanOutcome.VALID,
                occurredAt,
                actor,
                device,
                clientIp,
                true,
                false,
                "Primer acceso registrado.");
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER', 'ACCESS_STAFF')")
    public Page<ScanAttemptListItem> findAttempts(
            Long eventId,
            String authenticatedLogin,
            Pageable pageable) {
        User actor = findActor(authenticatedLogin);
        return attemptRepository.findVisible(
                        eventId,
                        organizerFilter(actor),
                        pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER', 'ACCESS_STAFF')")
    public AccessDashboardSummary getSummary(
            Long eventId,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Long organizerId = organizerFilter(actor);
        long cancelled = attemptRepository.countVisibleByOutcome(
                ScanOutcome.CANCELLED,
                eventId,
                organizerId);
        long counterfeit = attemptRepository.countVisibleByOutcome(
                ScanOutcome.COUNTERFEIT,
                eventId,
                organizerId);
        long expired = attemptRepository.countVisibleByOutcome(
                ScanOutcome.EXPIRED,
                eventId,
                organizerId);
        return new AccessDashboardSummary(
                eventRepository.sumVisibleCapacity(eventId, organizerId),
                ticketRepository.countVisible(eventId, organizerId),
                attemptRepository.countVisibleFirstAccesses(
                        eventId,
                        organizerId),
                ticketRepository.countVisibleByStatus(
                        TicketStatus.ACTIVE,
                        eventId,
                        organizerId),
                cancelled + counterfeit + expired,
                attemptRepository.countVisibleByOutcome(
                        ScanOutcome.DUPLICATE,
                        eventId,
                        organizerId),
                attemptRepository.countVisibleByOutcome(
                        ScanOutcome.REENTRY,
                        eventId,
                        organizerId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER', 'ACCESS_STAFF')")
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

    private ScanResultView record(
            DigitalTicket ticket,
            String rawCodeHash,
            ScanOutcome outcome,
            LocalDateTime occurredAt,
            User actor,
            String device,
            String ipAddress,
            boolean firstAccess,
            boolean duplicate,
            String notes) {
        attemptRepository.save(new TicketScanAttempt(
                ticket,
                rawCodeHash,
                outcome,
                occurredAt,
                actor,
                device,
                ipAddress,
                firstAccess,
                duplicate,
                notes));
        return new ScanResultView(
                outcome,
                outcome.accepted(),
                outcomeMessage(outcome),
                ticket == null ? null : ticket.getUniqueCode(),
                ticket == null ? null : ticket.getAttendeeName(),
                ticket == null ? null : ticket.getEvent().getTitle(),
                ticket == null ? null : ticket.getTicketTypeName(),
                ticket == null ? null : ticket.getZone(),
                ticket == null ? null : ticket.getSeat(),
                occurredAt);
    }

    private ScanAttemptListItem toListItem(TicketScanAttempt attempt) {
        return new ScanAttemptListItem(
                attempt.getId(),
                attempt.getTicket() == null
                        ? "No identificado"
                        : attempt.getTicket().getUniqueCode(),
                attempt.getEvent() == null
                        ? "Sin evento"
                        : attempt.getEvent().getTitle(),
                attempt.getOutcome(),
                attempt.getOccurredAt(),
                attempt.getScannedBy().getFullName(),
                attempt.getDeviceIdentifier(),
                attempt.getIpAddress(),
                attempt.isFirstAccess(),
                attempt.isDuplicateAttempt(),
                attempt.getNotes());
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private Long organizerFilter(User actor) {
        return actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;
    }

    private String normalizeDevice(String value) {
        String normalized = value == null ? "Navegador web" : value.trim();
        if (normalized.isBlank()) {
            return "Navegador web";
        }
        return normalized.substring(0, Math.min(normalized.length(), 120));
    }

    private String normalizeIp(String value) {
        String normalized = value == null ? "desconocida" : value.trim();
        if (normalized.isBlank()) {
            return "desconocida";
        }
        return normalized.substring(0, Math.min(normalized.length(), 45));
    }

    private String outcomeMessage(ScanOutcome outcome) {
        return switch (outcome) {
            case VALID -> "Entrada válida. Acceso registrado.";
            case REENTRY -> "Reingreso autorizado.";
            case DUPLICATE -> "Acceso rechazado: la entrada ya fue utilizada.";
            case CANCELLED -> "Acceso rechazado: la entrada está cancelada.";
            case COUNTERFEIT -> "Acceso rechazado: QR inválido o falsificado.";
            case EXPIRED -> "Acceso rechazado: la entrada está vencida.";
        };
    }
}
