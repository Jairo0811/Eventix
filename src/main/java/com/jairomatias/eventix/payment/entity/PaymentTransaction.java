package com.jairomatias.eventix.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 40)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private PaymentTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "response_message", length = 300)
    private String responseMessage;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_by_id", nullable = false)
    private User processedBy;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(
            Sale sale,
            String transactionReference,
            PaymentProvider provider,
            PaymentTransactionType transactionType,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            String externalReference,
            String responseMessage,
            LocalDateTime processedAt,
            User processedBy) {
        this.sale = sale;
        this.transactionReference = transactionReference;
        this.provider = provider;
        this.transactionType = transactionType;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.externalReference = externalReference;
        this.responseMessage = responseMessage;
        this.processedAt = processedAt;
        this.processedBy = processedBy;
    }

    public Sale getSale() {
        return sale;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public PaymentTransactionType getTransactionType() {
        return transactionType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public User getProcessedBy() {
        return processedBy;
    }
}
