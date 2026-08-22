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

    @Column(nullable = false)
    private boolean active = true;

    protected EligibilityGroup() {
    }

    public EligibilityGroup(
            Event event,
            String name,
            EligibilityGroupType groupType,
            Integer maxRelatedPeople) {
        this.event = event;
        this.name = name;
        this.groupType = groupType;
        this.maxRelatedPeople = maxRelatedPeople;
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

    public boolean isActive() {
        return active;
    }
}
