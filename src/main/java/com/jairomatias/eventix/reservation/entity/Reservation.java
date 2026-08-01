package com.jairomatias.eventix.reservation.entity;

import java.time.LocalDateTime;

import com.jairomatias.eventix.event.entity.Event;
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

@Entity
@Table(name = "reservations")
public class Reservation extends AuditableEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 24)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "attendee_first_name", nullable = false, length = 80)
    private String attendeeFirstName;

    @Column(name = "attendee_last_name", nullable = false, length = 80)
    private String attendeeLastName;

    @Column(name = "attendee_email", nullable = false, length = 160)
    private String attendeeEmail;

    @Column(name = "attendee_phone", nullable = false, length = 30)
    private String attendeePhone;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserved_by_id", nullable = false)
    private User reservedBy;

    protected Reservation() {
    }

    public Reservation(
            String referenceCode,
            Event event,
            String attendeeFirstName,
            String attendeeLastName,
            String attendeeEmail,
            String attendeePhone,
            int quantity,
            LocalDateTime expiresAt,
            User reservedBy) {
        this.referenceCode = referenceCode;
        this.event = event;
        this.attendeeFirstName = attendeeFirstName;
        this.attendeeLastName = attendeeLastName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.reservedBy = reservedBy;
    }

    public void updatePending(
            String attendeeFirstName,
            String attendeeLastName,
            String attendeeEmail,
            String attendeePhone,
            int quantity) {
        this.attendeeFirstName = attendeeFirstName;
        this.attendeeLastName = attendeeLastName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
        this.quantity = quantity;
    }

    public void confirm(LocalDateTime confirmedAt) {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public void cancel(String reason, LocalDateTime cancelledAt) {
        this.status = ReservationStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = cancelledAt;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public Event getEvent() {
        return event;
    }

    public String getAttendeeFirstName() {
        return attendeeFirstName;
    }

    public String getAttendeeLastName() {
        return attendeeLastName;
    }

    public String getAttendeeFullName() {
        return attendeeFirstName + " " + attendeeLastName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public String getAttendeePhone() {
        return attendeePhone;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public User getReservedBy() {
        return reservedBy;
    }
}
