package com.jairomatias.eventix.venue.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "venue_rows")
public class VenueRow extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSection section;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected VenueRow() {
    }

    public VenueRow(VenueSection section, String code, String label, int sortOrder) {
        this.section = section;
        update(code, label, sortOrder, true);
    }

    public void update(String code, String label, int sortOrder, boolean active) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public VenueSection getSection() {
        return section;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
