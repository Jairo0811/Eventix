package com.jairomatias.eventix.venue.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.dto.EventSectionPricingForm;
import com.jairomatias.eventix.venue.dto.EventSectionPricingView;
import com.jairomatias.eventix.venue.dto.TicketSeatingRule;
import com.jairomatias.eventix.venue.dto.TicketTypePricingOption;
import com.jairomatias.eventix.venue.dto.VenueSectionPricingOption;
import com.jairomatias.eventix.venue.entity.EventSectionPricing;
import com.jairomatias.eventix.venue.entity.VenueSection;
import com.jairomatias.eventix.venue.entity.VenueSectionType;
import com.jairomatias.eventix.venue.repository.EventSectionPricingRepository;
import com.jairomatias.eventix.venue.repository.VenueSectionRepository;

@Service
public class EventSectionPricingService {

    private final EventRepository eventRepository;
    private final VenueSectionRepository sectionRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventSectionPricingRepository pricingRepository;
    private final UserRepository userRepository;

    public EventSectionPricingService(
            EventRepository eventRepository,
            VenueSectionRepository sectionRepository,
            TicketTypeRepository ticketTypeRepository,
            EventSectionPricingRepository pricingRepository,
            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.sectionRepository = sectionRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.pricingRepository = pricingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public TicketSeatingRule resolveRule(Event event, TicketType ticketType) {
        EventSeatingMode mode = effectiveMode(event);
        if (mode == EventSeatingMode.GENERAL_ADMISSION) {
            return new TicketSeatingRule(false, null, ticketType.getPrice());
        }

        EventSectionPricing pricing = pricingRepository
                .findByEvent_IdAndTicketType_Id(event.getId(), ticketType.getId())
                .orElse(null);

        if (pricing == null) {
            if (mode == EventSeatingMode.RESERVED_SEATING) {
                throw new BusinessRuleException(
                        "El tipo de entrada '" + ticketType.getName()
                                + "' todavía no está asignado a una sección reservada.");
            }
            return new TicketSeatingRule(false, null, ticketType.getPrice());
        }

        VenueSection section = pricing.getSection();
        ensurePricingBelongsToEventVenue(event, section);
        if (!section.isActive()) {
            throw new BusinessRuleException(
                    "La sección configurada para esta entrada está inactiva.");
        }
        if (mode == EventSeatingMode.RESERVED_SEATING
                && section.getSectionType() != VenueSectionType.RESERVED_SEATING) {
            throw new BusinessRuleException(
                    "Un evento RESERVED_SEATING solo puede vender entradas asociadas a secciones reservadas.");
        }

        boolean requiresHold = section.getSectionType() == VenueSectionType.RESERVED_SEATING;
        return new TicketSeatingRule(
                requiresHold,
                requiresHold ? section.getId() : null,
                pricing.effectivePrice());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<EventSectionPricingView> list(Long eventId, String authenticatedLogin) {
        Event event = findManagedEvent(eventId, authenticatedLogin);
        return pricingRepository
                .findAllByEvent_IdOrderBySection_SortOrderAscTicketType_NameAsc(event.getId())
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<VenueSectionPricingOption> sectionOptions(
            Long eventId,
            String authenticatedLogin) {
        Event event = findManagedEvent(eventId, authenticatedLogin);
        if (event.getVenueDefinition() == null) {
            return List.of();
        }
        return sectionRepository
                .findAllByVenueIdAndActiveTrueOrderBySortOrderAscNameAsc(
                        event.getVenueDefinition().getId())
                .stream()
                .map(section -> new VenueSectionPricingOption(
                        section.getId(),
                        section.getCode(),
                        section.getName(),
                        section.getSectionType()))
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<TicketTypePricingOption> ticketTypeOptions(
            Long eventId,
            String authenticatedLogin) {
        findManagedEvent(eventId, authenticatedLogin);
        return ticketTypeRepository
                .findAllByEvent_IdAndActiveTrueOrderByNameAsc(eventId)
                .stream()
                .map(ticketType -> new TicketTypePricingOption(
                        ticketType.getId(),
                        ticketType.getName(),
                        ticketType.getPrice()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void save(
            Long eventId,
            EventSectionPricingForm form,
            String authenticatedLogin) {
        Event event = findManagedEvent(eventId, authenticatedLogin);
        if (effectiveMode(event) == EventSeatingMode.GENERAL_ADMISSION) {
            throw new BusinessRuleException(
                    "Configura el evento como RESERVED_SEATING o MIXED antes de asignar secciones.");
        }
        if (event.getVenueDefinition() == null) {
            throw new BusinessRuleException(
                    "El evento debe tener un recinto estructurado antes de configurar secciones y precios.");
        }

        VenueSection section = sectionRepository.findById(form.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la sección seleccionada."));
        TicketType ticketType = ticketTypeRepository.findDetailedById(form.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada seleccionado."));

        ensurePricingBelongsToEventVenue(event, section);
        if (!ticketType.getEvent().getId().equals(eventId) || !ticketType.isActive()) {
            throw new BusinessRuleException(
                    "El tipo de entrada no pertenece al evento o está inactivo.");
        }
        if (!section.isActive()) {
            throw new BusinessRuleException("La sección seleccionada está inactiva.");
        }
        if (effectiveMode(event) == EventSeatingMode.RESERVED_SEATING
                && section.getSectionType() != VenueSectionType.RESERVED_SEATING) {
            throw new BusinessRuleException(
                    "Un evento RESERVED_SEATING solo admite secciones de asientos reservados.");
        }

        BigDecimal priceOverride = normalizePrice(form.getPriceOverride());
        EventSectionPricing pricing;
        if (form.getPricingId() == null) {
            ensureUniqueMapping(eventId, section.getId(), ticketType.getId(), null);
            pricing = new EventSectionPricing(event, section, ticketType, priceOverride);
        } else {
            pricing = pricingRepository.findById(form.getPricingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró la configuración de precio solicitada."));
            if (!pricing.getEvent().getId().equals(eventId)) {
                throw new BusinessRuleException(
                        "La configuración de precio no pertenece al evento indicado.");
            }
            ensureUniqueMapping(eventId, section.getId(), ticketType.getId(), pricing.getId());
            pricing.update(section, ticketType, priceOverride);
        }
        pricingRepository.save(pricing);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void remove(Long eventId, Long pricingId, String authenticatedLogin) {
        findManagedEvent(eventId, authenticatedLogin);
        EventSectionPricing pricing = pricingRepository.findById(pricingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la configuración de precio solicitada."));
        if (!pricing.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException(
                    "La configuración de precio no pertenece al evento indicado.");
        }
        pricingRepository.delete(pricing);
    }

    private Event findManagedEvent(Long eventId, String authenticatedLogin) {
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        User actor = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        authenticatedLogin,
                        authenticatedLogin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
        ensureCanManage(event, actor);
        return event;
    }

    private void ensureCanManage(Event event, User actor) {
        if (actor.getRole().getName() == RoleName.ADMINISTRATOR) {
            return;
        }
        if (actor.getRole().getName() == RoleName.ORGANIZER
                && event.getOrganizer().getId().equals(actor.getId())) {
            return;
        }
        throw new BusinessRuleException(
                "No tienes permiso para administrar el seating de este evento.");
    }

    private void ensurePricingBelongsToEventVenue(Event event, VenueSection section) {
        if (event.getVenueDefinition() == null
                || !section.getVenue().getId().equals(event.getVenueDefinition().getId())) {
            throw new BusinessRuleException(
                    "La sección seleccionada no pertenece al recinto configurado para el evento.");
        }
    }

    private void ensureUniqueMapping(
            Long eventId,
            Long sectionId,
            Long ticketTypeId,
            Long pricingId) {
        boolean sectionUsed = pricingId == null
                ? pricingRepository.existsByEvent_IdAndSection_Id(eventId, sectionId)
                : pricingRepository.existsByEvent_IdAndSection_IdAndIdNot(
                        eventId, sectionId, pricingId);
        if (sectionUsed) {
            throw new BusinessRuleException(
                    "La sección ya está asociada a otro tipo de entrada en este evento.");
        }

        boolean ticketTypeUsed = pricingId == null
                ? pricingRepository.existsByEvent_IdAndTicketType_Id(eventId, ticketTypeId)
                : pricingRepository.existsByEvent_IdAndTicketType_IdAndIdNot(
                        eventId, ticketTypeId, pricingId);
        if (ticketTypeUsed) {
            throw new BusinessRuleException(
                    "El tipo de entrada ya está asociado a otra sección en este evento.");
        }
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("El precio por sección no puede ser negativo.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private EventSectionPricingView toView(EventSectionPricing pricing) {
        return new EventSectionPricingView(
                pricing.getId(),
                pricing.getSection().getId(),
                pricing.getSection().getCode(),
                pricing.getSection().getName(),
                pricing.getSection().getSectionType(),
                pricing.getTicketType().getId(),
                pricing.getTicketType().getName(),
                pricing.getTicketType().getPrice(),
                pricing.getPriceOverride(),
                pricing.effectivePrice());
    }

    private EventSeatingMode effectiveMode(Event event) {
        return event.getSeatingMode() == null
                ? EventSeatingMode.GENERAL_ADMISSION
                : event.getSeatingMode();
    }
}
