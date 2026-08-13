package com.jairomatias.eventix.home.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.home.dto.HomeEventView;

@Service
public class EventDiscoveryService {

    private static final int UPCOMING_EVENTS_LIMIT = 8;
    private static final int DISCOVERY_EVENTS_LIMIT = 7;

    private final EventRepository eventRepository;

    public EventDiscoveryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeEventView> upcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findAllByStatusAndStartAtAfterOrderByStartAtAsc(
                EventStatus.PUBLISHED,
                now,
                PageRequest.of(0, UPCOMING_EVENTS_LIMIT))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeEventView> nextSevenEvents() {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findAllByStatusAndStartAtAfterOrderByStartAtAsc(
                EventStatus.PUBLISHED,
                now,
                PageRequest.of(0, DISCOVERY_EVENTS_LIMIT))
                .stream()
                .map(this::toView)
                .toList();
    }

    private HomeEventView toView(Event event) {
        return new HomeEventView(
                event.getId(),
                event.getTitle(),
                event.getCategory().getName(),
                event.getStartAt(),
                event.getVenue(),
                event.getCoverImageUrl(),
                event.isFreeEvent(),
                event.getBasePrice());
    }
}
