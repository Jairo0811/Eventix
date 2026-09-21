package com.jairomatias.eventix.venue.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.dto.EventVenueConfigurationForm;
import com.jairomatias.eventix.venue.dto.VenueOption;
import com.jairomatias.eventix.venue.entity.Venue;
import com.jairomatias.eventix.venue.repository.EventSeatInventoryRepository;
import com.jairomatias.eventix.venue.repository.VenueRepository;

@Service
public class DefaultEventVenueConfigurationService
        implements EventVenueConfigurationService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final EventSeatInventoryRepository inventoryRepository;

    public DefaultEventVenueConfigurationService(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            UserRepository userRepository,
            EventSeatInventoryRepository inventoryRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public EventVenueConfigurationForm getForm(
            Long eventId,
            String authenticatedLogin) {
        Event event = findEvent(eventId);
        ensureCanManage(event, findActor(authenticatedLogin));

        EventVenueConfigurationForm form = new EventVenueConfigurationForm();
        form.setVenueId(event.getVenueDefinition() == null
                ? null
                : event.getVenueDefinition().getId());
        form.setSeatingMode(event.getSeatingMode());
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<VenueOption> getActiveVenues() {
        return venueRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(venue -> new VenueOption(
                        venue.getId(),
                        venue.getName(),
                        venue.getCity(),
                        venue.getCountryCode()))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void configure(
            Long eventId,
            EventVenueConfigurationForm form,
            String authenticatedLogin) {
        Event event = eventRepository.findDetailedByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        ensureCanManage(event, findActor(authenticatedLogin));

        if (inventoryRepository.existsByEvent_Id(eventId)) {
            throw new BusinessRuleException(
                    "No se puede cambiar el recinto o modo de asientos después de inicializar el inventario.");
        }

        EventSeatingMode mode = form.getSeatingMode() == null
                ? EventSeatingMode.GENERAL_ADMISSION
                : form.getSeatingMode();

        if (mode == EventSeatingMode.GENERAL_ADMISSION) {
            Venue venue = form.getVenueId() == null
                    ? null
                    : findActiveVenue(form.getVenueId());
            event.configureVenue(venue, mode);
            return;
        }

        if (form.getVenueId() == null) {
            throw new BusinessRuleException(
                    "Selecciona un recinto estructurado para asientos reservados o modo mixto.");
        }

        event.configureVenue(findActiveVenue(form.getVenueId()), mode);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
    }

    private Venue findActiveVenue(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El recinto seleccionado no existe."));
        if (!venue.isActive()) {
            throw new BusinessRuleException(
                    "El recinto seleccionado está inactivo.");
        }
        return venue;
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
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
                "No tienes permiso para configurar este evento.");
    }
}
