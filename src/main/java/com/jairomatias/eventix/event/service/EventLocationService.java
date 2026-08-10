package com.jairomatias.eventix.event.service;

import org.springframework.stereotype.Service;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;

@Service
class EventLocationService {

    private final EventRepository eventRepository;

    EventLocationService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    void updateGoogleMapsUrl(Long eventId, String googleMapsUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El evento solicitado no existe."));
        event.updateGoogleMapsUrl(normalizeNullable(googleMapsUrl));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
