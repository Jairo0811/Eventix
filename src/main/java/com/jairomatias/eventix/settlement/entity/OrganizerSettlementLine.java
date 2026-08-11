package com.jairomatias.eventix.settlement.entity;

import java.math.BigDecimal;

import com.jairomatias.eventix.sale.entity.Sale;
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
@Table(name = "organizer_settlement_lines")
public class OrganizerSettlementLine extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private OrganizerSettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 20)
    private SettlementLineType lineType;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(
            name = "platform_commission",
            nullable = false,
            precision = 12,
            scale = 2)
    private BigDecimal platformCommission;

    @Column(name = "organizer_net", nullable = false, precision = 12, scale = 2)
    private BigDecimal organizerNet;

    @Column(nullable = false)
    private boolean active = true;

    protected OrganizerSettlementLine() {
    }

    public static OrganizerSettlementLine sale(
            OrganizerSettlement settlement,
            Sale sale) {
        return new OrganizerSettlementLine(
                settlement,
                sale,
                SettlementLineType.SALE,
                sale.getSubtotal(),
                sale.getDiscountTotal(),
                BigDecimal.ZERO,
                sale.getPlatformFeeAmount(),
                sale.getOrganizerNetAmount());
    }

    public static OrganizerSettlementLine refund(
            OrganizerSettlement settlement,
            Sale sale) {
        return new OrganizerSettlementLine(
                settlement,
                sale,
                SettlementLineType.REFUND,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                sale.getTotal(),
                sale.getPlatformFeeAmount().negate(),
                sale.getOrganizerNetAmount().negate());
    }

    private OrganizerSettlementLine(
            OrganizerSettlement settlement,
            Sale sale,
            SettlementLineType lineType,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal platformCommission,
            BigDecimal organizerNet) {
        this.settlement = settlement;
        this.sale = sale;
        this.lineType = lineType;
        this.grossAmount = grossAmount;
        this.discountAmount = discountAmount;
        this.refundAmount = refundAmount;
        this.platformCommission = platformCommission;
        this.organizerNet = organizerNet;
    }

    public void deactivate() {
        active = false;
    }

    public Sale getSale() {
        return sale;
    }

    public SettlementLineType getLineType() {
        return lineType;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public BigDecimal getOrganizerNet() {
        return organizerNet;
    }

    public boolean isActive() {
        return active;
    }
}
