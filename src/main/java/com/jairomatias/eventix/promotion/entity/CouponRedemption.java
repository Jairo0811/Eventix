package com.jairomatias.eventix.promotion.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    private Sale sale;

    @Column(name = "buyer_email", nullable = false, length = 160)
    private String buyerEmail;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponRedemptionStatus status;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(
            Coupon coupon,
            Sale sale,
            String buyerEmail,
            BigDecimal discountAmount,
            LocalDateTime reservedAt) {
        this.coupon = coupon;
        this.sale = sale;
        this.buyerEmail = buyerEmail;
        this.discountAmount = discountAmount;
        this.status = CouponRedemptionStatus.RESERVED;
        this.reservedAt = reservedAt;
    }

    public void consume(LocalDateTime at) {
        if (status == CouponRedemptionStatus.RESERVED) {
            status = CouponRedemptionStatus.CONSUMED;
            consumedAt = at;
        }
    }

    public boolean release(LocalDateTime at) {
        if (status != CouponRedemptionStatus.RESERVED) {
            return false;
        }
        status = CouponRedemptionStatus.RELEASED;
        releasedAt = at;
        return true;
    }

    public CouponRedemptionStatus getStatus() {
        return status;
    }

    public Coupon getCoupon() {
        return coupon;
    }
}
