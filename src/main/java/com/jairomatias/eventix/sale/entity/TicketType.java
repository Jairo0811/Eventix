package com.jairomatias.eventix.sale.entity;

import java.math.BigDecimal;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_types")
public class TicketType extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketTypeCategory category;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean active = true;

    protected TicketType() {
    }

    public TicketType(
            Event event,
            TicketTypeCategory category,
            String name,
            BigDecimal price,
            int capacity) {
        this.event = event;
        update(category, name, price, capacity, true);
    }

    public void update(
            TicketTypeCategory category,
            String name,
            BigDecimal price,
            int capacity,
            boolean active) {
        this.category = category;
        this.name = name;
        this.price = price;
        this.capacity = capacity;
        this.active = active;
    }

    public Event getEvent() {
        return event;
    }

    public TicketTypeCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }
}
