package com.jairomatias.eventix.venue.entity;

import java.time.LocalDateTime;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "event_seat_inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_event_seat_inventory_event_seat",
                columnNames = {"event_id", "seat_id"}))
public class EventSeatInventory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private VenueSeat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventSeatStatus status = EventSeatStatus.AVAILABLE;

    @Column(name = "hold_token", length = 64)
    private String holdToken;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    protected EventSeatInventory() {
    }

    public EventSeatInventory(Event event, VenueSeat seat) {
        this.event = event;
        this.seat = seat;
    }

    public void hold(String token, LocalDateTime expiresAt) {
        this.status = EventSeatStatus.HELD;
        this.holdToken = token;
        this.holdExpiresAt = expiresAt;
        this.sale = null;
    }

    public void release() {
        this.status = EventSeatStatus.AVAILABLE;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.sale = null;
    }

    public void sell(Sale sale) {
        this.status = EventSeatStatus.SOLD;
        this.sale = sale;
        this.holdToken = null;
        this.holdExpiresAt = null;
    }

    public void block() {
        this.status = EventSeatStatus.BLOCKED;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.sale = null;
    }

    public boolean isHeldAndExpired(LocalDateTime now) {
        return status == EventSeatStatus.HELD
                && holdExpiresAt != null
                && !holdExpiresAt.isAfter(now);
    }

    public Event getEvent() { return event; }
    public VenueSeat getSeat() { return seat; }
    public EventSeatStatus getStatus() { return status; }
    public String getHoldToken() { return holdToken; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public Sale getSale() { return sale; }
}
