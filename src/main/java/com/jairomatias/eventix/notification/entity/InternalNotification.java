package com.jairomatias.eventix.notification.entity;

import java.time.LocalDateTime;

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
@Table(name = "internal_notifications")
public class InternalNotification extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private InternalNotificationType notificationType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected InternalNotification() {
    }

    public InternalNotification(
            User recipient,
            InternalNotificationType notificationType,
            String title,
            String message,
            String targetUrl) {
        this.recipient = recipient;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
    }

    public void markRead(LocalDateTime at) {
        if (readAt == null) {
            readAt = at;
        }
    }

    public User getRecipient() {
        return recipient;
    }

    public InternalNotificationType getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
