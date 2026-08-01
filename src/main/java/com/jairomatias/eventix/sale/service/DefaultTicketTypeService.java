package com.jairomatias.eventix.sale.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.dto.TicketTypeForm;
import com.jairomatias.eventix.sale.dto.TicketTypeView;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultTicketTypeService implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final SaleItemRepository saleItemRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public DefaultTicketTypeService(
            TicketTypeRepository ticketTypeRepository,
            SaleItemRepository saleItemRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {
        this(
                ticketTypeRepository,
                saleItemRepository,
                eventRepository,
                userRepository,
                Clock.systemDefaultZone());
    }

    DefaultTicketTypeService(
            TicketTypeRepository ticketTypeRepository,
            SaleItemRepository saleItemRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            Clock clock) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.saleItemRepository = saleItemRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<TicketTypeView> findByEvent(
            Long eventId,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Event event = findEvent(eventId);
        ensureCanManage(event, actor);

        return ticketTypeRepository
                .findAllByEvent_IdOrderByNameAsc(eventId)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public TicketTypeForm getCreateForm(
            Long eventId,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Event event = findEvent(eventId);
        ensureCanManage(event, actor);
        ensureEditable(event);

        TicketTypeForm form = new TicketTypeForm();
        form.setCapacity(event.getCapacity());
        form.setPrice(event.getBasePrice());
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public TicketTypeForm getUpdateForm(
            Long id,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        TicketType ticketType = findTicketType(id);
        ensureCanManage(ticketType.getEvent(), actor);
        ensureEditable(ticketType.getEvent());

        TicketTypeForm form = new TicketTypeForm();
        form.setCategory(ticketType.getCategory());
        form.setName(ticketType.getName());
        form.setPrice(ticketType.getPrice());
        form.setCapacity(ticketType.getCapacity());
        form.setActive(ticketType.isActive());
        return form;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public Long create(
            Long eventId,
            TicketTypeForm form,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Event event = findEventForUpdate(eventId);
        ensureCanManage(event, actor);
        ensureEditable(event);
        validate(form, event, null);

        TicketType ticketType = new TicketType(
                event,
                form.getCategory(),
                form.getName().trim(),
                normalizedPrice(form),
                form.getCapacity());
        return ticketTypeRepository.save(ticketType).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void update(
            Long id,
            TicketTypeForm form,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        TicketType current = findTicketType(id);
        Event event = findEventForUpdate(current.getEvent().getId());
        TicketType ticketType = findTicketTypeForUpdate(id);
        ensureCanManage(event, actor);
        ensureEditable(event);
        validate(form, event, id);

        long allocated = saleItemRepository.sumAllocatedQuantity(id);
        if (form.getCapacity() < allocated) {
            throw new BusinessRuleException(
                    "La capacidad no puede ser menor que las "
                    + allocated
                    + " entradas asignadas a ventas activas.");
        }

        ticketType.update(
                form.getCategory(),
                form.getName().trim(),
                normalizedPrice(form),
                form.getCapacity(),
                form.isActive());
    }

    private void validate(
            TicketTypeForm form,
            Event event,
            Long excludedId) {
        if (form == null || form.getCategory() == null) {
            throw new BusinessRuleException(
                    "Selecciona una categoría de entrada.");
        }
        if (form.getName() == null || form.getName().isBlank()) {
            throw new BusinessRuleException(
                    "El nombre del tipo de entrada es obligatorio.");
        }
        if (form.getCapacity() <= 0
                || form.getCapacity() > event.getCapacity()) {
            throw new BusinessRuleException(
                    "La capacidad del tipo debe estar entre 1 y "
                    + event.getCapacity()
                    + ".");
        }

        BigDecimal price = normalizedPrice(form);
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "El precio no puede ser negativo.");
        }
        if (event.isFreeEvent() && price.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException(
                    "Los tipos de entrada de un evento gratuito deben tener precio cero.");
        }
        if (form.getCategory() == TicketTypeCategory.COMPLIMENTARY
                && price.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException(
                    "Las entradas de cortesía deben tener precio cero.");
        }
        if (!event.isFreeEvent()
                && form.getCategory() != TicketTypeCategory.COMPLIMENTARY
                && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "Los tipos de un evento de pago deben tener un precio mayor que cero, salvo Cortesía.");
        }

        boolean duplicate = excludedId == null
                ? ticketTypeRepository.existsByEvent_IdAndNameIgnoreCase(
                        event.getId(),
                        form.getName().trim())
                : ticketTypeRepository
                        .existsByEvent_IdAndNameIgnoreCaseAndIdNot(
                                event.getId(),
                                form.getName().trim(),
                                excludedId);
        if (duplicate) {
            throw new DuplicateResourceException(
                    "name",
                    "Ya existe un tipo de entrada con ese nombre para el evento.");
        }
    }

    private BigDecimal normalizedPrice(TicketTypeForm form) {
        return form.getPrice() == null
                ? BigDecimal.ZERO
                : form.getPrice().setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void ensureEditable(Event event) {
        if (event.getStatus() != EventStatus.DRAFT
                && event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Solo se configuran entradas para eventos en borrador o publicados.");
        }
        if (!event.getStartAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException(
                    "No se pueden modificar entradas después de iniciar el evento.");
        }
    }

    private void ensureCanManage(Event event, User actor) {
        RoleName role = actor.getRole().getName();
        boolean administrator = role == RoleName.ADMINISTRATOR;
        boolean owner = role == RoleName.ORGANIZER
                && event.getOrganizer().getId().equals(actor.getId());
        if (!administrator && !owner) {
            throw new BusinessRuleException(
                    "No tienes permiso para configurar las entradas de este evento.");
        }
    }

    private TicketTypeView toView(TicketType ticketType) {
        int allocated = Math.toIntExact(
                saleItemRepository.sumAllocatedQuantity(ticketType.getId()));
        return new TicketTypeView(
                ticketType.getId(),
                ticketType.getEvent().getId(),
                ticketType.getEvent().getTitle(),
                ticketType.getCategory(),
                ticketType.getName(),
                ticketType.getPrice(),
                ticketType.getCapacity(),
                allocated,
                Math.max(ticketType.getCapacity() - allocated, 0),
                ticketType.isActive());
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
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

    private TicketType findTicketType(Long id) {
        return ticketTypeRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada solicitado."));
    }

    private TicketType findTicketTypeForUpdate(Long id) {
        return ticketTypeRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada solicitado."));
    }
}
