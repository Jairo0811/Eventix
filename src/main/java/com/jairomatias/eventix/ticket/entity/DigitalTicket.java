package com.jairomatias.eventix.ticket.entity;

import java.time.LocalDateTime;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleItem;
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
@Table(name = "digital_tickets")
public class DigitalTicket extends AuditableEntity {

    @Column(name = "unique_code", nullable = false, unique = true, length = 32)
    private String uniqueCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private SaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "attendee_name", nullable = false, length = 161)
    private String attendeeName;

    @Column(name = "attendee_email", nullable = false, length = 160)
    private String attendeeEmail;

    @Column(name = "ticket_type_name", nullable = false, length = 80)
    private String ticketTypeName;

    @Column(length = 80)
    private String zone;

    @Column(length = 40)
    private String seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.ACTIVE;

    @Column(name = "anti_fraud_code", nullable = false, unique = true, length = 32)
    private String antiFraudCode;

    @Column(name = "signed_payload_hash", nullable = false, length = 64)
    private String signedPayloadHash;

    @Column(name = "digital_signature", nullable = false, length = 180)
    private String digitalSignature;

    @Column(name = "signature_key_id", nullable = false, length = 80)
    private String signatureKeyId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "pass_updated_at", nullable = false)
    private LocalDateTime passUpdatedAt;

    protected DigitalTicket() {
    }

    public DigitalTicket(
            String uniqueCode,
            Sale sale,
            SaleItem saleItem,
            int sequenceNumber,
            String antiFraudCode,
            String signedPayloadHash,
            String digitalSignature,
            String signatureKeyId,
            LocalDateTime issuedAt) {
        this.uniqueCode = uniqueCode;
        this.sale = sale;
        this.saleItem = saleItem;
        this.event = sale.getEvent();
        this.sequenceNumber = sequenceNumber;
        this.attendeeName = sale.getBuyerName();
        this.attendeeEmail = sale.getBuyerEmail();
        this.ticketTypeName = saleItem.getTicketTypeName();
        this.zone = saleItem.getTicketTypeName();
        this.antiFraudCode = antiFraudCode;
        this.signedPayloadHash = signedPayloadHash;
        this.digitalSignature = digitalSignature;
        this.signatureKeyId = signatureKeyId;
        this.issuedAt = issuedAt;
        this.passUpdatedAt = issuedAt;
    }

    public void markUsed(LocalDateTime at) {
        status = TicketStatus.USED;
        usedAt = at;
        passUpdatedAt = at;
    }

    public void cancel(String reason, LocalDateTime at) {
        status = TicketStatus.CANCELLED;
        cancellationReason = reason;
        cancelledAt = at;
        passUpdatedAt = at;
    }

    public void expire(LocalDateTime at) {
        status = TicketStatus.EXPIRED;
        passUpdatedAt = at;
    }

    public void touchPass(LocalDateTime at) {
        passUpdatedAt = at;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public Sale getSale() {
        return sale;
    }

    public SaleItem getSaleItem() {
        return saleItem;
    }

    public Event getEvent() {
        return event;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public String getZone() {
        return zone;
    }

    public String getSeat() {
        return seat;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getAntiFraudCode() {
        return antiFraudCode;
    }

    public String getSignedPayloadHash() {
        return signedPayloadHash;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public String getSignatureKeyId() {
        return signatureKeyId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getPassUpdatedAt() {
        return passUpdatedAt;
    }
}
