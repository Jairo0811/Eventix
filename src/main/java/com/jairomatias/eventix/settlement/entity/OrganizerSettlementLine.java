package com.jairomatias.eventix.settlement.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.jairomatias.eventix.payment.entity.PaymentTransaction;
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

    private static final int MONEY_SCALE = 2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private OrganizerSettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

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
        BigDecimal originalCommission = money(sale.getTotal()
                .multiply(sale.getPlatformFeeRate()));
        BigDecimal originalOrganizerNet = money(
                sale.getTotal().subtract(originalCommission));
        return new OrganizerSettlementLine(
                settlement,
                sale,
                null,
                SettlementLineType.SALE,
                sale.getSubtotal(),
                sale.getDiscountTotal(),
                BigDecimal.ZERO,
                originalCommission,
                originalOrganizerNet);
    }

    public static OrganizerSettlementLine refund(
            OrganizerSettlement settlement,
            PaymentTransaction transaction) {
        Sale sale = transaction.getSale();
        BigDecimal refundAmount = money(transaction.getAmount());
        BigDecimal commissionReversal = money(
                refundAmount.multiply(sale.getPlatformFeeRate()));
        BigDecimal organizerReversal = money(
                refundAmount.subtract(commissionReversal));
        return new OrganizerSettlementLine(
                settlement,
                sale,
                transaction,
                SettlementLineType.REFUND,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                refundAmount,
                commissionReversal.negate(),
                organizerReversal.negate());
    }

    private OrganizerSettlementLine(
            OrganizerSettlement settlement,
            Sale sale,
            PaymentTransaction paymentTransaction,
            SettlementLineType lineType,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal platformCommission,
            BigDecimal organizerNet) {
        this.settlement = settlement;
        this.sale = sale;
        this.paymentTransaction = paymentTransaction;
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

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public Sale getSale() {
        return sale;
    }

    public PaymentTransaction getPaymentTransaction() {
        return paymentTransaction;
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
