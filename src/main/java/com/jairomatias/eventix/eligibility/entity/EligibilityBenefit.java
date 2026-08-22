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

    @Column(nullable = false)
    private boolean active = true;

    protected EligibilityBenefit() {
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

    public boolean isActive() {
        return active;
    }
}
