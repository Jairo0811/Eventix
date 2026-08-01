package com.jairomatias.eventix.reservation.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.dto.CancellationForm;
import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.reservation.dto.ReservationDetailsView;
import com.jairomatias.eventix.reservation.dto.ReservationForm;
import com.jairomatias.eventix.reservation.dto.ReservationListItem;
import com.jairomatias.eventix.reservation.dto.ReservationMetrics;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.mapper.ReservationMapper;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultReservationService implements ReservationService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationReferenceGenerator referenceGenerator;
    private final ReservationProperties properties;
    private final Clock clock;

    @Autowired
    public DefaultReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            ReservationMapper reservationMapper,
            ReservationReferenceGenerator referenceGenerator,
            ReservationProperties properties) {
        this(
                reservationRepository,
                eventRepository,
                userRepository,
                reservationMapper,
                referenceGenerator,
                properties,
                Clock.systemDefaultZone());
    }

    DefaultReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            ReservationMapper reservationMapper,
            ReservationReferenceGenerator referenceGenerator,
            ReservationProperties properties,
            Clock clock) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.reservationMapper = reservationMapper;
        this.referenceGenerator = referenceGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public Page<ReservationListItem> findAll(
            String term,
            ReservationStatus status,
            Long eventId,
            String authenticatedLogin,
            Pageable pageable) {
        LocalDateTime now = now();
        reservationRepository.expirePendingBefore(now);

        User actor = findActor(authenticatedLogin);
        Long organizerId = actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;

        return reservationRepository.search(
                        normalizeSearchTerm(term),
                        status,
                        eventId,
                        organizerId,
                        pageable)
                .map(reservationMapper::toListItem);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public ReservationDetailsView findById(
            Long id,
            String authenticatedLogin) {
        reservationRepository.expirePendingBefore(now());
        User actor = findActor(authenticatedLogin);
        Reservation reservation = findEntity(id);
        ensureCanView(reservation, actor);
        return reservationMapper.toDetailsView(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public ReservationForm getCreateForm(
            Long eventId,
            String authenticatedLogin) {
        findActor(authenticatedLogin);
        ReservationForm form = new ReservationForm();
        form.setEventId(eventId);

        if (eventId != null) {
            ensureReservable(findEvent(eventId), now());
        }
        return form;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public ReservationForm getUpdateForm(
            Long id,
            String authenticatedLogin) {
        LocalDateTime now = now();
        User actor = findActor(authenticatedLogin);
        findEventForUpdate(findReservationEventId(id));
        Reservation reservation = findEntityForUpdate(id);
        ensureCanManage(reservation, actor);
        ensurePendingAndCurrent(reservation, now);
        return reservationMapper.toForm(reservation);
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
        return events.stream().map(this::toEventOption).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public List<EventReservationOption> findReservableEvents() {
        return eventRepository
                .findAllByStatusAndStartAtAfterOrderByStartAtAsc(
                        EventStatus.PUBLISHED,
                        now())
                .stream()
                .map(this::toEventOption)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public Long create(
            ReservationForm form,
            String authenticatedLogin) {
        validateForm(form);
        LocalDateTime now = now();
        User actor = findActor(authenticatedLogin);
        Event event = findEventForUpdate(form.getEventId());

        ensureReservable(event, now);
        reservationRepository.expirePendingForEvent(event.getId(), now);

        String attendeeEmail = normalizeEmail(form.getAttendeeEmail());
        ensureNoDuplicate(event.getId(), attendeeEmail, now, null);
        ensureAvailability(
                event,
                form.getQuantity(),
                reservationRepository.sumOccupiedSeats(
                        event.getId(),
                        now));

        Reservation reservation = new Reservation(
                nextReferenceCode(),
                event,
                form.getAttendeeFirstName().trim(),
                form.getAttendeeLastName().trim(),
                attendeeEmail,
                form.getAttendeePhone().trim(),
                form.getQuantity(),
                now.plus(properties.getHoldDuration()),
                actor);

        return reservationRepository.save(reservation).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public void update(
            Long id,
            ReservationForm form,
            String authenticatedLogin) {
        validateForm(form);
        LocalDateTime now = now();
        User actor = findActor(authenticatedLogin);
        Event event = findEventForUpdate(findReservationEventId(id));
        Reservation reservation = findEntityForUpdate(id);
        ensureCanManage(reservation, actor);
        ensurePendingAndCurrent(reservation, now);

        if (!reservation.getEvent().getId().equals(form.getEventId())) {
            throw new BusinessRuleException(
                    "No se puede cambiar el evento de una reservación existente.");
        }

        ensureReservable(event, now);

        String attendeeEmail = normalizeEmail(form.getAttendeeEmail());
        ensureNoDuplicate(event.getId(), attendeeEmail, now, id);
        ensureAvailability(
                event,
                form.getQuantity(),
                reservationRepository.sumOccupiedSeatsExcluding(
                        event.getId(),
                        id,
                        now));

        reservation.updatePending(
                form.getAttendeeFirstName().trim(),
                form.getAttendeeLastName().trim(),
                attendeeEmail,
                form.getAttendeePhone().trim(),
                form.getQuantity());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public void confirm(
            Long id,
            String authenticatedLogin) {
        LocalDateTime now = now();
        User actor = findActor(authenticatedLogin);
        Event event = findEventForUpdate(findReservationEventId(id));
        Reservation reservation = findEntityForUpdate(id);
        ensureCanManage(reservation, actor);
        ensurePendingAndCurrent(reservation, now);
        ensureReservable(event, now);
        reservation.confirm(now);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public void cancel(
            Long id,
            CancellationForm form,
            String authenticatedLogin) {
        if (form == null
                || form.getReason() == null
                || form.getReason().isBlank()) {
            throw new BusinessRuleException(
                    "Indica el motivo de la cancelación.");
        }
        LocalDateTime now = now();
        User actor = findActor(authenticatedLogin);
        findEventForUpdate(findReservationEventId(id));
        Reservation reservation = findEntityForUpdate(id);
        ensureCanManage(reservation, actor);

        if (!reservation.getStatus().isActive()) {
            throw new BusinessRuleException(
                    "Solo se pueden cancelar reservaciones pendientes o confirmadas.");
        }
        if (!reservation.getEvent().getStartAt().isAfter(now)) {
            throw new BusinessRuleException(
                    "No se puede cancelar una reservación después de iniciar el evento.");
        }
        reservation.cancel(form.getReason().trim(), now);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public ReservationMetrics getEventMetrics(
            Long eventId,
            String authenticatedLogin) {
        LocalDateTime now = now();
        reservationRepository.expirePendingForEvent(eventId, now);

        User actor = findActor(authenticatedLogin);
        Event event = findEvent(eventId);
        ensureCanViewEvent(event, actor);

        int pendingSeats = Math.toIntExact(
                reservationRepository.sumPendingSeats(
                        eventId,
                        now));
        int confirmedSeats = Math.toIntExact(
                reservationRepository.sumConfirmedSeats(eventId));
        int occupiedSeats = pendingSeats + confirmedSeats;
        int availableSeats = Math.max(
                event.getCapacity() - occupiedSeats,
                0);
        int occupancy = event.getCapacity() == 0
                ? 0
                : (int) Math.round(
                        occupiedSeats * 100.0 / event.getCapacity());

        return new ReservationMetrics(
                event.getId(),
                event.getTitle(),
                event.getCapacity(),
                pendingSeats,
                confirmedSeats,
                availableSeats,
                occupancy);
    }

    @Override
    @Transactional
    public int expirePendingReservations() {
        return reservationRepository.expirePendingBefore(now());
    }

    private User findActor(String authenticatedLogin) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        authenticatedLogin,
                        authenticatedLogin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private Event findEvent(Long id) {
        return eventRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
    }

    private Event findEventForUpdate(Long id) {
        return eventRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
    }

    private Reservation findEntity(Long id) {
        return reservationRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reservación solicitada."));
    }

    private Reservation findEntityForUpdate(Long id) {
        return reservationRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reservación solicitada."));
    }

    private Long findReservationEventId(Long reservationId) {
        return reservationRepository.findEventIdById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reservación solicitada."));
    }

    private void ensureCanView(Reservation reservation, User actor) {
        ensureCanViewEvent(reservation.getEvent(), actor);
    }

    private void ensureCanViewEvent(Event event, User actor) {
        RoleName role = actor.getRole().getName();
        if (role == RoleName.ADMINISTRATOR || role == RoleName.OPERATOR) {
            return;
        }
        if (role == RoleName.ORGANIZER
                && event.getOrganizer().getId().equals(actor.getId())) {
            return;
        }
        throw new BusinessRuleException(
                "No tienes permiso para consultar estas reservaciones.");
    }

    private void ensureCanManage(Reservation reservation, User actor) {
        RoleName role = actor.getRole().getName();
        if (role != RoleName.ADMINISTRATOR && role != RoleName.OPERATOR) {
            throw new BusinessRuleException(
                    "No tienes permiso para administrar esta reservación.");
        }
    }

    private void ensureReservable(Event event, LocalDateTime now) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Solo se pueden reservar entradas para eventos publicados.");
        }
        if (!event.getStartAt().isAfter(now)) {
            throw new BusinessRuleException(
                    "No se pueden reservar entradas para un evento iniciado o finalizado.");
        }
    }

    private void ensurePendingAndCurrent(
            Reservation reservation,
            LocalDateTime now) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessRuleException(
                    "La operación solo está disponible para reservaciones pendientes.");
        }
        if (!reservation.getExpiresAt().isAfter(now)) {
            throw new BusinessRuleException(
                    "La reservación expiró y sus cupos ya fueron liberados.");
        }
    }

    private void ensureNoDuplicate(
            Long eventId,
            String attendeeEmail,
            LocalDateTime now,
            Long excludedId) {
        if (reservationRepository.existsActiveDuplicate(
                eventId,
                attendeeEmail,
                now,
                excludedId)) {
            throw new BusinessRuleException(
                    "Ya existe una reservación activa para este correo y evento.");
        }
    }

    private void ensureAvailability(
            Event event,
            int requestedQuantity,
            long occupiedSeats) {
        long availableSeats = event.getCapacity() - occupiedSeats;
        if (requestedQuantity > availableSeats) {
            throw new BusinessRuleException(
                    "No hay cupos suficientes. Disponibles: "
                            + Math.max(availableSeats, 0)
                            + ".");
        }
    }

    private void validateForm(ReservationForm form) {
        if (form == null || form.getEventId() == null) {
            throw new BusinessRuleException(
                    "Selecciona un evento.");
        }
        if (isBlank(form.getAttendeeFirstName())
                || isBlank(form.getAttendeeLastName())
                || isBlank(form.getAttendeeEmail())
                || isBlank(form.getAttendeePhone())) {
            throw new BusinessRuleException(
                    "Completa los datos obligatorios del asistente.");
        }
        if (form.getQuantity() < 1 || form.getQuantity() > 100) {
            throw new BusinessRuleException(
                    "La cantidad debe estar entre 1 y 100 entradas.");
        }
    }

    private String nextReferenceCode() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String candidate = referenceGenerator.generate();
            if (!reservationRepository.existsByReferenceCode(candidate)) {
                return candidate;
            }
        }
        throw new BusinessRuleException(
                "No fue posible generar una referencia única. Inténtalo nuevamente.");
    }

    private EventReservationOption toEventOption(Event event) {
        return new EventReservationOption(
                event.getId(),
                event.getTitle(),
                event.getStartAt(),
                event.getCapacity());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSearchTerm(String term) {
        return term == null ? "" : term.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
