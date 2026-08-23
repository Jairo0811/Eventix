package com.jairomatias.eventix.sale.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.promotion.entity.Coupon;
import com.jairomatias.eventix.promotion.entity.DiscountType;
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

    public static final BigDecimal DEFAULT_PLATFORM_FEE_RATE = new BigDecimal("0.0500");

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_discount_type", length = 20)
    private DiscountType couponDiscountType;

    @Column(name = "coupon_discount_value", precision = 12, scale = 2)
    private BigDecimal couponDiscountValue;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    @Column(name = "eligibility_benefit_id")
    private Long eligibilityBenefitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_benefit_type", length = 30)
    private EligibilityBenefitType eligibilityBenefitType;

    @Column(name = "eligibility_discount_value", precision = 12, scale = 2)
    private BigDecimal eligibilityDiscountValue;

    @Column(name = "eligibility_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal eligibilityDiscountAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal platformFeeRate;

    @Column(
            name = "platform_fee_amount",
            precision = 12,
            scale = 2,
            insertable = false,
            updatable = false)
    private BigDecimal platformFeeAmount;

    @Column(
            name = "organizer_net_amount",
            precision = 12,
            scale = 2,
            insertable = false,
            updatable = false)
    private BigDecimal organizerNetAmount;

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
        this.discountTotal = BigDecimal.ZERO;
        this.couponDiscountAmount = BigDecimal.ZERO;
        this.eligibilityDiscountAmount = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.platformFeeRate = DEFAULT_PLATFORM_FEE_RATE;
    }

    public void addItem(TicketType ticketType, int quantity) {
        SaleItem item = new SaleItem(this, ticketType, quantity);
        items.add(item);
        recalculateTotals();
    }

    public void applyCoupon(
            Coupon appliedCoupon,
            BigDecimal discountAmount) {
        if (eligibilityDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Los descuentos de cupón y elegibilidad no pueden acumularse en la misma venta.");
        }
        validateDiscountAmount(discountAmount);
        coupon = appliedCoupon;
        couponCode = appliedCoupon.getCode();
        couponDiscountType = appliedCoupon.getDiscountType();
        couponDiscountValue = appliedCoupon.getValue();
        couponDiscountAmount = discountAmount;
        recalculateTotals();
    }

    public void applyEligibilityDiscount(
            Long benefitId,
            EligibilityBenefitType benefitType,
            BigDecimal configuredValue,
            BigDecimal discountAmount) {
        if (couponDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Los descuentos de cupón y elegibilidad no pueden acumularse en la misma venta.");
        }
        if (benefitId == null || benefitType == null || !isMonetaryBenefit(benefitType)) {
            throw new IllegalArgumentException("El beneficio de elegibilidad monetario es inválido.");
        }
        validateDiscountAmount(discountAmount);
        eligibilityBenefitId = benefitId;
        eligibilityBenefitType = benefitType;
        eligibilityDiscountValue = configuredValue;
        eligibilityDiscountAmount = discountAmount;
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

    private void validateDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null
                || discountAmount.compareTo(BigDecimal.ZERO) < 0
                || discountAmount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "El descuento debe estar entre cero y el subtotal.");
        }
    }

    private boolean isMonetaryBenefit(EligibilityBenefitType benefitType) {
        return benefitType == EligibilityBenefitType.PERCENTAGE_DISCOUNT
                || benefitType == EligibilityBenefitType.FIXED_DISCOUNT
                || benefitType == EligibilityBenefitType.FREE_ENTRY;
    }

    private void recalculateTotals() {
        subtotal = items.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        discountTotal = couponDiscountAmount
                .add(eligibilityDiscountAmount)
                .min(subtotal)
                .max(BigDecimal.ZERO);
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

    public String getCouponCode() {
        return couponCode;
    }

    public DiscountType getCouponDiscountType() {
        return couponDiscountType;
    }

    public BigDecimal getCouponDiscountValue() {
        return couponDiscountValue;
    }

    public BigDecimal getCouponDiscountAmount() {
        return couponDiscountAmount;
    }

    public Long getEligibilityBenefitId() {
        return eligibilityBenefitId;
    }

    public EligibilityBenefitType getEligibilityBenefitType() {
        return eligibilityBenefitType;
    }

    public BigDecimal getEligibilityDiscountValue() {
        return eligibilityDiscountValue;
    }

    public BigDecimal getEligibilityDiscountAmount() {
        return eligibilityDiscountAmount;
    }

    public BigDecimal getPlatformFeeRate() {
        return platformFeeRate;
    }

    public BigDecimal getPlatformFeeAmount() {
        return platformFeeAmount;
    }

    public BigDecimal getOrganizerNetAmount() {
        return organizerNetAmount;
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
