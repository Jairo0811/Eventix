package com.jairomatias.eventix.eligibility.entity;

import com.jairomatias.eventix.event.entity.Event;
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
@Table(name = "eligibility_groups")
public class EligibilityGroup extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 30)
    private EligibilityGroupType groupType;

    @Column(name = "max_related_people")
    private Integer maxRelatedPeople;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_promotion_id")
    private SchoolPromotion schoolPromotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_key", length = 40)
    private EligibilityGroupSystemKey systemKey;

    @Column(nullable = false)
    private boolean active = true;

    protected EligibilityGroup() {
    }

    public EligibilityGroup(
            Event event,
            String name,
            EligibilityGroupType groupType,
            Integer maxRelatedPeople) {
        this(event, name, groupType, maxRelatedPeople, null, null);
    }

    public EligibilityGroup(
            Event event,
            String name,
            EligibilityGroupType groupType,
            Integer maxRelatedPeople,
            SchoolPromotion schoolPromotion) {
        this(event, name, groupType, maxRelatedPeople, schoolPromotion, null);
    }

    public EligibilityGroup(
            Event event,
            String name,
            EligibilityGroupType groupType,
            Integer maxRelatedPeople,
            SchoolPromotion schoolPromotion,
            EligibilityGroupSystemKey systemKey) {
        this.event = event;
        this.systemKey = systemKey;
        update(name, groupType, maxRelatedPeople, schoolPromotion);
    }

    public void update(String name, EligibilityGroupType groupType, Integer maxRelatedPeople) {
        update(name, groupType, maxRelatedPeople, null);
    }

    public void update(
            String name,
            EligibilityGroupType groupType,
            Integer maxRelatedPeople,
            SchoolPromotion schoolPromotion) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del grupo es obligatorio.");
        }
        if (groupType == null) {
            throw new IllegalArgumentException("El tipo de grupo es obligatorio.");
        }
        if (maxRelatedPeople != null && maxRelatedPeople < 0) {
            throw new IllegalArgumentException("El límite de relacionados no puede ser negativo.");
        }
        this.name = name.trim();
        this.groupType = groupType;
        this.maxRelatedPeople = groupType == EligibilityGroupType.FAMILY ? maxRelatedPeople : null;
        this.schoolPromotion = groupType == EligibilityGroupType.PROMOTION_MEMBER ? schoolPromotion : null;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void releaseSystemKey() {
        this.systemKey = null;
    }

    public Event getEvent() {
        return event;
    }

    public String getName() {
        return name;
    }

    public EligibilityGroupType getGroupType() {
        return groupType;
    }

    public Integer getMaxRelatedPeople() {
        return maxRelatedPeople;
    }

    public SchoolPromotion getSchoolPromotion() {
        return schoolPromotion;
    }

    public EligibilityGroupSystemKey getSystemKey() {
        return systemKey;
    }

    public boolean isActive() {
        return active;
    }
}
