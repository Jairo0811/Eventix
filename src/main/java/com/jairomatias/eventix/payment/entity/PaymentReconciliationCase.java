package com.jairomatias.eventix.payment.entity;

import java.math.BigDecimal;
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

/**
 * Independent comparison ledger. This entity never approves charges,
 * refunds, settles money, or issues tickets.
 */
@Entity
@Table(name = "payment_reconciliation_cases",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_payment_reconciliation_provider_event",
                columnNames = {"provider", "external_event_id"}))
public class PaymentReconciliationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "external_event_id", nullable = false, length = 160)
    private String externalEventId;

    @Column(name = "sale_reference", nullable = false, length = 24)
    private String saleReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private PaymentTransactionType transactionType;

    @Column(name = "external_reference", nullable = false, length = 120)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_status", nullable = false, length = 20)
    private PaymentStatus reportedStatus;

    @Column(name = "reported_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal reportedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    protected PaymentReconciliationCase() {
    }

    public PaymentReconciliationCase(
            PaymentProvider provider,
            String externalEventId,
            String saleReference,
            PaymentTransactionType transactionType,
            String externalReference,
            PaymentStatus reportedStatus,
            BigDecimal reportedAmount,
            String currency,
            String outcome,
            String reasonCode,
            LocalDateTime reviewedAt) {
        this.provider = provider;
        this.externalEventId = externalEventId;
        this.saleReference = saleReference;
        this.transactionType = transactionType;
        this.externalReference = externalReference;
        this.reportedStatus = reportedStatus;
        this.reportedAmount = reportedAmount;
        this.currency = currency;
        this.outcome = outcome;
        this.reasonCode = reasonCode;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() { return id; }
    public PaymentProvider getProvider() { return provider; }
    public String getExternalEventId() { return externalEventId; }
    public String getSaleReference() { return saleReference; }
    public PaymentTransactionType getTransactionType() { return transactionType; }
    public String getExternalReference() { return externalReference; }
    public PaymentStatus getReportedStatus() { return reportedStatus; }
    public BigDecimal getReportedAmount() { return reportedAmount; }
    public String getCurrency() { return currency; }
    public String getOutcome() { return outcome; }
    public String getReasonCode() { return reasonCode; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}
