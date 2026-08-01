package com.jairomatias.eventix.sale.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.shared.entity.AuditableEntity;
import com.jairomatias.eventix.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sales")
public class Sale extends AuditableEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 24)
    private String referenceCode;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "buyer_name", nullable = false, length = 161)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false, length = 160)
    private String buyerEmail;

    @Column(name = "buyer_phone", nullable = false, length = 30)
    private String buyerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status = SaleStatus.PENDING;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sold_by_id", nullable = false)
    private User soldBy;

    @OneToMany(
            mappedBy = "sale",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private final List<SaleItem> items = new ArrayList<>();

    protected Sale() {
    }

    public Sale(
            String referenceCode,
            Reservation reservation,
            String currency,
            User soldBy) {
        this.referenceCode = referenceCode;
        this.reservation = reservation;
        this.event = reservation.getEvent();
        this.buyerName = reservation.getAttendeeFullName();
        this.buyerEmail = reservation.getAttendeeEmail();
        this.buyerPhone = reservation.getAttendeePhone();
        this.currency = currency;
        this.soldBy = soldBy;
        this.subtotal = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
    }

    public void addItem(TicketType ticketType, int quantity) {
        SaleItem item = new SaleItem(this, ticketType, quantity);
        items.add(item);
        recalculateTotals();
    }

    public void markPaid(LocalDateTime paidAt) {
        this.status = SaleStatus.PAID;
        this.paidAt = paidAt;
    }

    public void markRefunded(String reason, LocalDateTime refundedAt) {
        this.status = SaleStatus.REFUNDED;
        this.refundReason = reason;
        this.refundedAt = refundedAt;
    }

    public void cancel(String reason, LocalDateTime cancelledAt) {
        this.status = SaleStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = cancelledAt;
    }

    private void recalculateTotals() {
        subtotal = items.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.subtract(discountTotal);
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Event getEvent() {
        return event;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public User getSoldBy() {
        return soldBy;
    }

    public List<SaleItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
