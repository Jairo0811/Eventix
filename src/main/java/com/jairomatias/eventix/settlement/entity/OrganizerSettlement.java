package com.jairomatias.eventix.settlement.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jairomatias.eventix.sale.entity.Sale;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "organizer_settlements")
public class OrganizerSettlement extends AuditableEntity {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "gross_sales", nullable = false, precision = 18, scale = 2)
    private BigDecimal grossSales = money(BigDecimal.ZERO);

    @Column(name = "discounts", nullable = false, precision = 18, scale = 2)
    private BigDecimal discounts = money(BigDecimal.ZERO);

    @Column(name = "refunds", nullable = false, precision = 18, scale = 2)
    private BigDecimal refunds = money(BigDecimal.ZERO);

    @Column(
            name = "platform_commission",
            nullable = false,
            precision = 18,
            scale = 2)
    private BigDecimal platformCommission = money(BigDecimal.ZERO);

    @Column(name = "organizer_net", nullable = false, precision = 18, scale = 2)
    private BigDecimal organizerNet = money(BigDecimal.ZERO);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "administrative_notes", length = 1000)
    private String administrativeNotes;

    @OneToMany(
            mappedBy = "settlement",
            cascade = CascadeType.ALL,
            orphanRemoval = false)
    private final List<OrganizerSettlementLine> lines = new ArrayList<>();

    protected OrganizerSettlement() {
    }

    public OrganizerSettlement(
            User organizer,
            LocalDate periodFrom,
            LocalDate periodTo,
            String administrativeNotes) {
        this.organizer = organizer;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.administrativeNotes = administrativeNotes;
    }

    public void addSale(Sale sale) {
        addLine(OrganizerSettlementLine.sale(this, sale));
    }

    public void addRefund(Sale sale) {
        addLine(OrganizerSettlementLine.refund(this, sale));
    }

    public void startProcessing(LocalDateTime at, String notes) {
        if (status != SettlementStatus.PENDING
                && status != SettlementStatus.FAILED) {
            throw new IllegalStateException(
                    "Solo una liquidación pendiente o fallida puede procesarse.");
        }
        status = SettlementStatus.PROCESSING;
        processedAt = at;
        administrativeNotes = notes;
    }

    public void markPaid(
            LocalDateTime at,
            String reference,
            String notes) {
        requireProcessing();
        status = SettlementStatus.PAID;
        paidAt = at;
        externalReference = reference;
        administrativeNotes = notes;
    }

    public void markFailed(LocalDateTime at, String notes) {
        requireProcessing();
        status = SettlementStatus.FAILED;
        processedAt = at;
        administrativeNotes = notes;
    }

    public void cancel(String notes) {
        if (status == SettlementStatus.PAID
                || status == SettlementStatus.CANCELLED) {
            throw new IllegalStateException(
                    "La liquidación ya no puede cancelarse.");
        }
        status = SettlementStatus.CANCELLED;
        administrativeNotes = notes;
        lines.forEach(OrganizerSettlementLine::deactivate);
    }

    private void addLine(OrganizerSettlementLine line) {
        lines.add(line);
        grossSales = money(grossSales.add(line.getGrossAmount()));
        discounts = money(discounts.add(line.getDiscountAmount()));
        refunds = money(refunds.add(line.getRefundAmount()));
        platformCommission = money(platformCommission.add(
                line.getPlatformCommission()));
        organizerNet = money(organizerNet.add(line.getOrganizerNet()));
    }

    private void requireProcessing() {
        if (status != SettlementStatus.PROCESSING) {
            throw new IllegalStateException(
                    "La liquidación debe estar en procesamiento.");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public User getOrganizer() {
        return organizer;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public BigDecimal getGrossSales() {
        return grossSales;
    }

    public BigDecimal getDiscounts() {
        return discounts;
    }

    public BigDecimal getRefunds() {
        return refunds;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public BigDecimal getOrganizerNet() {
        return organizerNet;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getAdministrativeNotes() {
        return administrativeNotes;
    }

    public List<OrganizerSettlementLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
