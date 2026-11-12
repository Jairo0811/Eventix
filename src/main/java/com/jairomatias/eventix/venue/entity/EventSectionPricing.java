package com.jairomatias.eventix.venue.entity;

import java.math.BigDecimal;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "event_section_pricing",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "UQ_event_section_pricing_event_section",
                    columnNames = {"event_id", "section_id"}),
            @UniqueConstraint(
                    name = "UQ_event_section_pricing_event_ticket_type",
                    columnNames = {"event_id", "ticket_type_id"})
        })
public class EventSectionPricing extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;

    protected EventSectionPricing() {
    }

    public EventSectionPricing(
            Event event,
            VenueSection section,
            TicketType ticketType,
            BigDecimal priceOverride) {
        this.event = event;
        update(section, ticketType, priceOverride);
    }

    public void update(
            VenueSection section,
            TicketType ticketType,
            BigDecimal priceOverride) {
        this.section = section;
        this.ticketType = ticketType;
        this.priceOverride = priceOverride;
    }

    public Event getEvent() {
        return event;
    }

    public VenueSection getSection() {
        return section;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public BigDecimal effectivePrice() {
        return priceOverride == null ? ticketType.getPrice() : priceOverride;
    }
}
