package com.jairomatias.eventix.ticket.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.event.EventChangedEvent;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.ticket.event.TicketPassChangedEvent;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;

@Component
public class EventTicketPassListener {

    private final DigitalTicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock = Clock.systemDefaultZone();

    public EventTicketPassListener(
            DigitalTicketRepository ticketRepository,
            ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onEventChanged(EventChangedEvent event) {
        LocalDateTime changedAt = LocalDateTime.now(clock).withNano(0);
        ticketRepository.findAllByEvent_IdOrderBySequenceNumberAsc(
                        event.eventId())
                .forEach(ticket -> {
                    if (ticket.getEvent().getStatus()
                            == EventStatus.CANCELLED) {
                        ticket.cancel(
                                "El evento fue cancelado.",
                                changedAt);
                    } else {
                        ticket.touchPass(changedAt);
                    }
                    eventPublisher.publishEvent(
                            new TicketPassChangedEvent(ticket.getId()));
                });
    }
}
