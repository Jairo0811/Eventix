package com.jairomatias.eventix.promotion.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupons")
public class Coupon extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 240)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "total_use_limit")
    private Integer totalUseLimit;

    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "minimum_subtotal", precision = 12, scale = 2)
    private BigDecimal minimumSubtotal;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_events",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    private final Set<Event> applicableEvents = new LinkedHashSet<>();

    protected Coupon() {
    }

    public Coupon(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal value,
            LocalDateTime startsAt,
            LocalDateTime expiresAt,
            boolean active,
            Integer totalUseLimit,
            Integer perUserLimit,
            BigDecimal minimumSubtotal,
            Set<Event> applicableEvents) {
        update(
                code,
                description,
                discountType,
                value,
                startsAt,
                expiresAt,
                active,
                totalUseLimit,
                perUserLimit,
                minimumSubtotal,
                applicableEvents);
    }

    public void update(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal value,
            LocalDateTime startsAt,
            LocalDateTime expiresAt,
            boolean active,
            Integer totalUseLimit,
            Integer perUserLimit,
            BigDecimal minimumSubtotal,
            Set<Event> applicableEvents) {
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.value = value;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.totalUseLimit = totalUseLimit;
        this.perUserLimit = perUserLimit;
        this.minimumSubtotal = minimumSubtotal;
        this.applicableEvents.clear();
        this.applicableEvents.addAll(applicableEvents);
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void reserveUse() {
        currentUses++;
    }

    public void releaseUse() {
        if (currentUses > 0) {
            currentUses--;
        }
    }

    public boolean appliesTo(Long eventId) {
        return applicableEvents.stream()
                .anyMatch(event -> event.getId().equals(eventId));
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getTotalUseLimit() {
        return totalUseLimit;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public BigDecimal getMinimumSubtotal() {
        return minimumSubtotal;
    }

    public Set<Event> getApplicableEvents() {
        return Collections.unmodifiableSet(applicableEvents);
    }
}
