package com.jairomatias.eventix.eligibility.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.sale.entity.TicketType;
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
@Table(name = "eligibility_benefits")
public class EligibilityBenefit extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private EligibilityGroup group;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 30)
    private EligibilityBenefitType benefitType;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_tickets_per_purchase")
    private Integer maxTicketsPerPurchase;

    @Column(name = "reserved_inventory")
    private Integer reservedInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id")
    private TicketType ticketType;

    @Column(name = "early_access_at")
    private LocalDateTime earlyAccessAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_key", length = 50)
    private EligibilityBenefitSystemKey systemKey;

    @Column(nullable = false)
    private boolean active = true;

    protected EligibilityBenefit() {
    }

    public EligibilityBenefit(
            EligibilityGroup group,
            EligibilityBenefitType benefitType,
            BigDecimal discountValue,
            Integer maxTicketsPerPurchase,
            Integer reservedInventory,
            TicketType ticketType,
            LocalDateTime earlyAccessAt) {
        this(group, benefitType, discountValue, maxTicketsPerPurchase, reservedInventory,
                ticketType, earlyAccessAt, null);
    }

    public EligibilityBenefit(
            EligibilityGroup group,
            EligibilityBenefitType benefitType,
            BigDecimal discountValue,
            Integer maxTicketsPerPurchase,
            Integer reservedInventory,
            TicketType ticketType,
            LocalDateTime earlyAccessAt,
            EligibilityBenefitSystemKey systemKey) {
        this.group = group;
        this.systemKey = systemKey;
        update(benefitType, discountValue, maxTicketsPerPurchase, reservedInventory, ticketType, earlyAccessAt);
    }

    public void update(
            EligibilityBenefitType benefitType,
            BigDecimal discountValue,
            Integer maxTicketsPerPurchase,
            Integer reservedInventory,
            TicketType ticketType,
            LocalDateTime earlyAccessAt) {
        this.benefitType = benefitType;
        this.discountValue = discountValue;
        this.maxTicketsPerPurchase = maxTicketsPerPurchase;
        this.reservedInventory = reservedInventory;
        this.ticketType = ticketType;
        this.earlyAccessAt = earlyAccessAt;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public EligibilityGroup getGroup() {
        return group;
    }

    public EligibilityBenefitType getBenefitType() {
        return benefitType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public Integer getMaxTicketsPerPurchase() {
        return maxTicketsPerPurchase;
    }

    public Integer getReservedInventory() {
        return reservedInventory;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public LocalDateTime getEarlyAccessAt() {
        return earlyAccessAt;
    }

    public EligibilityBenefitSystemKey getSystemKey() {
        return systemKey;
    }

    public boolean isActive() {
        return active;
    }
}
