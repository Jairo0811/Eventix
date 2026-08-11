package com.jairomatias.eventix.notification.entity;

import java.time.Duration;
import java.time.LocalDateTime;

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
@Table(name = "event_reminder_deliveries")
public class EventReminderDelivery extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "recipient_email", nullable = false, length = 160)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderDeliveryStatus status = ReminderDeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error_type", length = 160)
    private String lastErrorType;

    protected EventReminderDelivery() {
    }

    public boolean isDue(LocalDateTime now) {
        return status == ReminderDeliveryStatus.PENDING
                || (status == ReminderDeliveryStatus.FAILED
                    && nextAttemptAt != null
                    && !nextAttemptAt.isAfter(now));
    }

    public void markSent(LocalDateTime at) {
        attemptCount++;
        lastAttemptAt = at;
        sentAt = at;
        nextAttemptAt = null;
        lastErrorType = null;
        status = ReminderDeliveryStatus.SENT;
    }

    public void markFailed(
            LocalDateTime at,
            Duration retryDelay,
            int maximumAttempts,
            String errorType) {
        attemptCount++;
        lastAttemptAt = at;
        nextAttemptAt = attemptCount < maximumAttempts
                ? at.plus(retryDelay)
                : null;
        lastErrorType = errorType;
        status = ReminderDeliveryStatus.FAILED;
    }

    public void markSkipped(LocalDateTime at) {
        lastAttemptAt = at;
        nextAttemptAt = null;
        status = ReminderDeliveryStatus.SKIPPED;
    }

    public Event getEvent() {
        return event;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public ReminderDeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public String getLastErrorType() {
        return lastErrorType;
    }
}
