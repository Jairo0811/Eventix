package com.jairomatias.eventix.payment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "payment_webhook_inbox",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_payment_webhook_inbox_provider_event",
                columnNames = {"provider", "external_event_id"}))
public class PaymentWebhookInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "external_event_id", nullable = false, length = 160)
    private String externalEventId;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected PaymentWebhookInbox() {
    }

    public PaymentWebhookInbox(
            PaymentProvider provider,
            String externalEventId,
            String payloadSha256,
            LocalDateTime verifiedAt) {
        this.provider = provider;
        this.externalEventId = externalEventId;
        this.payloadSha256 = payloadSha256;
        this.verifiedAt = verifiedAt;
        this.status = "VERIFIED";
    }

    public Long getId() { return id; }
    public PaymentProvider getProvider() { return provider; }
    public String getExternalEventId() { return externalEventId; }
    public String getPayloadSha256() { return payloadSha256; }
    public String getStatus() { return status; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
