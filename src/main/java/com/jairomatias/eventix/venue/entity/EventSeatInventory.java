package com.jairomatias.eventix.venue.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.shared.entity.AuditableEntity;
import com.jairomatias.eventix.user.entity.User;

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

    @Column(name = "section_code_snapshot", nullable = false, length = 40)
    private String sectionCodeSnapshot;

    @Column(name = "section_name_snapshot", nullable = false, length = 120)
    private String sectionNameSnapshot;

    @Column(name = "row_code_snapshot", nullable = false, length = 40)
    private String rowCodeSnapshot;

    @Column(name = "seat_number_snapshot", nullable = false, length = 20)
    private String seatNumberSnapshot;

    @Column(name = "seat_label_snapshot", nullable = false, length = 80)
    private String seatLabelSnapshot;

    @Column(name = "accessible_snapshot", nullable = false)
    private boolean accessibleSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventSeatStatus status = EventSeatStatus.AVAILABLE;

    @Column(name = "hold_token", length = 64)
    private String holdToken;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    protected EventSeatInventory() {
    }

    public EventSeatInventory(Event event, VenueSeat seat) {
        this.event = Objects.requireNonNull(event, "El evento es obligatorio.");
        this.seat = Objects.requireNonNull(seat, "El asiento es obligatorio.");

        VenueRow row = Objects.requireNonNull(
                seat.getRow(),
                "La fila del asiento es obligatoria.");
        VenueSection section = Objects.requireNonNull(
                row.getSection(),
                "La sección del asiento es obligatoria.");

        this.sectionCodeSnapshot = section.getCode();
        this.sectionNameSnapshot = section.getName();
        this.rowCodeSnapshot = row.getCode();
        this.seatNumberSnapshot = seat.getSeatNumber();
        this.seatLabelSnapshot = seat.getLabel();
        this.accessibleSnapshot = seat.isAccessible();
    }

    public void hold(String token, LocalDateTime expiresAt, User owner) {
        this.status = EventSeatStatus.HELD;
        this.holdToken = token;
        this.holdExpiresAt = expiresAt;
        this.heldByUser = owner;
        this.sale = null;
    }

    public void release() {
        this.status = EventSeatStatus.AVAILABLE;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.heldByUser = null;
        this.sale = null;
    }

    public void sell(Sale sale) {
        this.status = EventSeatStatus.SOLD;
        this.sale = sale;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.heldByUser = null;
    }

    public void block() {
        this.status = EventSeatStatus.BLOCKED;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.heldByUser = null;
        this.sale = null;
    }

    public boolean isHeldAndExpired(LocalDateTime now) {
        return status == EventSeatStatus.HELD
                && holdExpiresAt != null
                && !holdExpiresAt.isAfter(now);
    }

    public boolean isHeldBy(Long userId) {
        return heldByUser != null
                && userId != null
                && userId.equals(heldByUser.getId());
    }

    public Event getEvent() { return event; }
    public VenueSeat getSeat() { return seat; }
    public String getSectionCodeSnapshot() { return sectionCodeSnapshot; }
    public String getSectionNameSnapshot() { return sectionNameSnapshot; }
    public String getRowCodeSnapshot() { return rowCodeSnapshot; }
    public String getSeatNumberSnapshot() { return seatNumberSnapshot; }
    public String getSeatLabelSnapshot() { return seatLabelSnapshot; }
    public boolean isAccessibleSnapshot() { return accessibleSnapshot; }
    public EventSeatStatus getStatus() { return status; }
    public String getHoldToken() { return holdToken; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public User getHeldByUser() { return heldByUser; }
    public Sale getSale() { return sale; }
}
