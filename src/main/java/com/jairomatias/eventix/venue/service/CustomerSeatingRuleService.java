package com.jairomatias.eventix.venue.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.venue.dto.TicketSeatingRule;

@Service
public class CustomerSeatingRuleService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventSectionPricingService sectionPricingService;

    public CustomerSeatingRuleService(
            EventRepository eventRepository,
            TicketTypeRepository ticketTypeRepository,
            EventSectionPricingService sectionPricingService) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.sectionPricingService = sectionPricingService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public TicketSeatingRule resolve(Long eventId, Long ticketTypeId) {
        if (ticketTypeId == null) {
            throw new BusinessRuleException(
                    "Selecciona primero un tipo de entrada para elegir asientos.");
        }

        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        TicketType ticketType = ticketTypeRepository.findDetailedById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entrada seleccionado."));

        if (!ticketType.getEvent().getId().equals(eventId) || !ticketType.isActive()) {
            throw new BusinessRuleException(
                    "El tipo de entrada seleccionado no pertenece al evento o está inactivo.");
        }

        TicketSeatingRule rule = sectionPricingService.resolveRule(event, ticketType);
        if (!rule.requiresSeatHold() || rule.sectionId() == null) {
            throw new BusinessRuleException(
                    "El tipo de entrada seleccionado no requiere asiento reservado.");
        }
        return rule;
    }
}
