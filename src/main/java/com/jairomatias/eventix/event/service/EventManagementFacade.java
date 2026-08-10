package com.jairomatias.eventix.event.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.dto.EventForm;

@Service
public class EventManagementFacade {

    private final EventService eventService;
    private final EventLocationService eventLocationService;

    public EventManagementFacade(
            EventService eventService,
            EventLocationService eventLocationService) {
        this.eventService = eventService;
        this.eventLocationService = eventLocationService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public Long create(EventForm form, String authenticatedLogin) {
        Long eventId = eventService.create(form, authenticatedLogin);
        eventLocationService.updateGoogleMapsUrl(
                eventId,
                form.getGoogleMapsUrl());
        return eventId;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void update(
            Long eventId,
            EventForm form,
            String authenticatedLogin) {
        eventService.update(eventId, form, authenticatedLogin);
        eventLocationService.updateGoogleMapsUrl(
                eventId,
                form.getGoogleMapsUrl());
    }
}
