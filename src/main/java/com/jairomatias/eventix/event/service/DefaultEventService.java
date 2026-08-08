package com.jairomatias.eventix.event.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.dto.EventDetailsView;
import com.jairomatias.eventix.event.dto.EventForm;
import com.jairomatias.eventix.event.dto.EventListItem;
import com.jairomatias.eventix.event.dto.OrganizerOption;
import com.jairomatias.eventix.event.event.EventChangedEvent;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.mapper.EventMapper;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultEventService implements EventService {

    private static final Set<EventStatus> OPERATIONAL_VISIBILITY =
            EnumSet.of(
                    EventStatus.PUBLISHED,
                    EventStatus.FINISHED);

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public DefaultEventService(
            EventRepository eventRepository,
            EventCategoryRepository categoryRepository,
            UserRepository userRepository,
            EventMapper eventMapper,
            ReservationRepository reservationRepository,
            ApplicationEventPublisher eventPublisher) {
        this(
                eventRepository,
                categoryRepository,
                userRepository,
                eventMapper,
                reservationRepository,
                eventPublisher,
                Clock.systemDefaultZone());
    }

    DefaultEventService(
            EventRepository eventRepository,
            EventCategoryRepository categoryRepository,
            UserRepository userRepository,
            EventMapper eventMapper,
            ReservationRepository reservationRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<EventListItem> findAll(
            String term,
            EventStatus status,
            Long categoryId,
            Long organizerId,
            String authenticatedLogin,
            Pageable pageable) {

        User actor = findActor(authenticatedLogin);
        RoleName role = actor.getRole().getName();

        Long effectiveOrganizerId = role == RoleName.ORGANIZER
                ? actor.getId()
                : organizerId;

        Set<EventStatus> visibleStatuses =
                role == RoleName.ADMINISTRATOR
                        || role == RoleName.ORGANIZER
                ? EnumSet.allOf(EventStatus.class)
                : OPERATIONAL_VISIBILITY;

        String normalizedTerm = term == null
                ? ""
                : term.trim();

        return eventRepository.search(
                        normalizedTerm,
                        status,
                        categoryId,
                        effectiveOrganizerId,
                        visibleStatuses,
                        pageable)
                .map(eventMapper::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public EventDetailsView findById(
            Long id,
            String authenticatedLogin) {

        User actor = findActor(authenticatedLogin);
        Event event = findEntity(id);
        ensureCanView(event, actor);
        return eventMapper.toDetailsView(event);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public EventForm getCreateForm(String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        EventForm form = new EventForm();
        LocalDateTime defaultStart =
                LocalDateTime.now(clock)
                        .plusDays(1)
                        .withSecond(0)
                        .withNano(0);

        form.setStartAt(defaultStart);
        form.setEndAt(defaultStart.plusHours(3));
        form.setOrganizerId(
                actor.getRole().getName() == RoleName.ORGANIZER
                        ? actor.getId()
                        : null);
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public EventForm getUpdateForm(
            Long id,
            String authenticatedLogin) {

        User actor = findActor(authenticatedLogin);
        Event event = findEntityForUpdate(id);
        ensureCanManage(event, actor);
        return eventMapper.toForm(event);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<OrganizerOption> findEligibleOrganizers(
            String authenticatedLogin) {

        User actor = findActor(authenticatedLogin);

        if (actor.getRole().getName() == RoleName.ORGANIZER) {
            return List.of(toOrganizerOption(actor));
        }

        return userRepository
                .findAllByRole_NameAndStatusOrderByLastNameAscFirstNameAsc(
                        RoleName.ORGANIZER,
                        UserStatus.ACTIVE)
                .stream()
                .map(this::toOrganizerOption)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public Long create(
            EventForm form,
            String authenticatedLogin) {

        User actor = findActor(authenticatedLogin);
        EventCategory category =
                findActiveCategory(form.getCategoryId());
        User organizer = resolveOrganizer(form, actor);

        validateBusinessRules(form, null);

        Event event = new Event(
                form.getTitle().trim(),
                form.getDescription().trim(),
                category,
                form.getStatus(),
                form.getStartAt(),
                form.getEndAt(),
                form.getVenue().trim(),
                form.getAddress().trim(),
                form.getCapacity(),
                organizer,
                normalizeNullable(form.getCoverImageUrl()),
                Boolean.TRUE.equals(form.getFreeEvent()),
                normalizedPrice(form));

        return eventRepository.save(event).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void update(
            Long id,
            EventForm form,
            String authenticatedLogin) {

        User actor = findActor(authenticatedLogin);
        Event event = findEntity(id);
        ensureCanManage(event, actor);

        EventCategory category =
                findCategoryForUpdate(form.getCategoryId(), event);
        User organizer = resolveOrganizer(form, actor);

        validateBusinessRules(form, event.getStatus());
        validateReservationConstraints(event, form);

        event.update(
                form.getTitle().trim(),
                form.getDescription().trim(),
                category,
                form.getStatus(),
                form.getStartAt(),
                form.getEndAt(),
                form.getVenue().trim(),
                form.getAddress().trim(),
                form.getCapacity(),
                organizer,
                normalizeNullable(form.getCoverImageUrl()),
                Boolean.TRUE.equals(form.getFreeEvent()),
                normalizedPrice(form));
        eventPublisher.publishEvent(new EventChangedEvent(event.getId()));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void delete(
            Long id,
            String authenticatedLogin) {

        User actor = findActor(authenticatedLogin);
        Event event = findEntityForUpdate(id);
        ensureCanManage(event, actor);

        if (event.getStatus() != EventStatus.DRAFT
                && event.getStatus() != EventStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Solo se pueden eliminar eventos "
                    + "en borrador o cancelados.");
        }

        if (reservationRepository.existsByEvent_Id(event.getId())) {
            throw new BusinessRuleException(
                    "No se puede eliminar un evento con historial de reservaciones.");
        }

        eventRepository.delete(event);
    }

    private void validateBusinessRules(
            EventForm form,
            EventStatus currentStatus) {

        if (form.getStartAt() == null || form.getEndAt() == null) {
            throw new BusinessRuleException(
                    "Las fechas de inicio y finalización son obligatorias.");
        }

        if (!form.getEndAt().isAfter(form.getStartAt())) {
            throw new BusinessRuleException(
                    "La finalización debe ser posterior al inicio.");
        }

        validateStatusTransition(currentStatus, form.getStatus());

        LocalDateTime now = LocalDateTime.now(clock);
        boolean publishing = form.getStatus() == EventStatus.PUBLISHED
                && currentStatus != EventStatus.PUBLISHED;

        if (publishing && !form.getStartAt().isAfter(now)) {
            throw new BusinessRuleException(
                    "Un evento se publica únicamente "
                    + "si todavía no ha iniciado.");
        }

        if (form.getStatus() == EventStatus.FINISHED
                && form.getEndAt().isAfter(now)) {
            throw new BusinessRuleException(
                    "No puedes finalizar un evento "
                    + "antes de su fecha de cierre.");
        }

        boolean freeEvent = Boolean.TRUE.equals(form.getFreeEvent());
        BigDecimal price = form.getBasePrice();

        if (!freeEvent
                && (price == null
                    || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessRuleException(
                    "Un evento de pago debe tener "
                    + "un precio base mayor que cero.");
        }
    }

    private void validateStatusTransition(
            EventStatus currentStatus,
            EventStatus resultingStatus) {

        if (resultingStatus == null) {
            throw new BusinessRuleException(
                    "Selecciona un estado para el evento.");
        }

        if (currentStatus == null) {
            if (resultingStatus != EventStatus.DRAFT
                    && resultingStatus != EventStatus.PUBLISHED) {
                throw new BusinessRuleException(
                        "Un evento nuevo debe guardarse "
                        + "como borrador o publicado.");
            }
            return;
        }

        boolean allowed = switch (currentStatus) {
            case DRAFT -> resultingStatus == EventStatus.DRAFT
                    || resultingStatus == EventStatus.PUBLISHED
                    || resultingStatus == EventStatus.CANCELLED;
            case PUBLISHED ->
                    resultingStatus == EventStatus.PUBLISHED
                    || resultingStatus == EventStatus.CANCELLED
                    || resultingStatus == EventStatus.FINISHED;
            case CANCELLED ->
                    resultingStatus == EventStatus.CANCELLED;
            case FINISHED ->
                    resultingStatus == EventStatus.FINISHED;
        };

        if (!allowed) {
            throw new BusinessRuleException(
                    "La transición de "
                    + currentStatus.getDisplayName().toLowerCase()
                    + " a "
                    + resultingStatus.getDisplayName().toLowerCase()
                    + " no está permitida.");
        }
    }

    private User resolveOrganizer(EventForm form, User actor) {
        if (actor.getRole().getName() == RoleName.ORGANIZER) {
            return actor;
        }

        User organizer = userRepository.findById(form.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El organizador seleccionado no existe."));

        if (organizer.getRole().getName() != RoleName.ORGANIZER
                || organizer.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "El responsable debe ser "
                    + "un organizador activo.");
        }

        return organizer;
    }

    private EventCategory findActiveCategory(Long id) {
        EventCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La categoría seleccionada no existe."));

        if (!category.isActive()) {
            throw new BusinessRuleException(
                    "La categoría seleccionada está inactiva.");
        }

        return category;
    }

    private EventCategory findCategoryForUpdate(
            Long id,
            Event event) {

        EventCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La categoría seleccionada no existe."));

        boolean keepsCurrentCategory =
                event.getCategory().getId().equals(category.getId());

        if (!category.isActive() && !keepsCurrentCategory) {
            throw new BusinessRuleException(
                    "La categoría seleccionada está inactiva.");
        }

        return category;
    }

    private Event findEntity(Long id) {
        return eventRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El evento solicitado no existe."));
    }

    private Event findEntityForUpdate(Long id) {
        return eventRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El evento solicitado no existe."));
    }

    private void validateReservationConstraints(
            Event event,
            EventForm form) {
        LocalDateTime now = LocalDateTime.now(clock);
        long occupiedSeats = reservationRepository.sumOccupiedSeats(
                event.getId(),
                now);

        if (form.getCapacity() < occupiedSeats) {
            throw new BusinessRuleException(
                    "La capacidad no puede ser menor que los "
                            + occupiedSeats
                            + " cupos actualmente ocupados.");
        }

        boolean cancelling = event.getStatus() != EventStatus.CANCELLED
                && form.getStatus() == EventStatus.CANCELLED;
        if (cancelling && reservationRepository.existsActiveByEvent(
                event.getId(),
                now)) {
            throw new BusinessRuleException(
                    "Cancela primero las reservaciones activas del evento.");
        }
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        login,
                        login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado no existe."));
    }

    private void ensureCanView(Event event, User actor) {
        RoleName role = actor.getRole().getName();
        boolean unrestricted = role == RoleName.ADMINISTRATOR
                || (role == RoleName.ORGANIZER
                    && event.getOrganizer().getId()
                            .equals(actor.getId()));
        boolean operationallyVisible =
                OPERATIONAL_VISIBILITY.contains(event.getStatus());

        if (!unrestricted && !operationallyVisible) {
            throw new ResourceNotFoundException(
                    "El evento solicitado no existe.");
        }
    }

    private void ensureCanManage(Event event, User actor) {
        RoleName role = actor.getRole().getName();
        boolean administrator = role == RoleName.ADMINISTRATOR;
        boolean owner = role == RoleName.ORGANIZER
                && event.getOrganizer().getId()
                        .equals(actor.getId());

        if (!administrator && !owner) {
            throw new BusinessRuleException(
                    "No tienes permiso para administrar este evento.");
        }
    }

    private OrganizerOption toOrganizerOption(User user) {
        return new OrganizerOption(
                user.getId(),
                user.getFullName(),
                user.getEmail());
    }

    private BigDecimal normalizedPrice(EventForm form) {
        return Boolean.TRUE.equals(form.getFreeEvent())
                ? BigDecimal.ZERO
                : form.getBasePrice();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
