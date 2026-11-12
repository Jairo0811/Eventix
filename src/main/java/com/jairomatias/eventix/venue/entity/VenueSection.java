package com.jairomatias.eventix.venue.entity;

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
@Table(name = "venue_sections")
public class VenueSection extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 24)
    private VenueSectionType sectionType;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected VenueSection() {
    }

    public VenueSection(
            Venue venue,
            String code,
            String name,
            VenueSectionType sectionType,
            int capacity,
            int sortOrder) {
        this.venue = venue;
        update(code, name, sectionType, capacity, sortOrder, true);
    }

    public void update(
            String code,
            String name,
            VenueSectionType sectionType,
            int capacity,
            int sortOrder,
            boolean active) {
        this.code = code;
        this.name = name;
        this.sectionType = sectionType;
        this.capacity = capacity;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public VenueSectionType getSectionType() {
        return sectionType;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
