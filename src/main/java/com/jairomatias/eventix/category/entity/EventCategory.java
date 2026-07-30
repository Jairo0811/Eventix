package com.jairomatias.eventix.category.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_categories")
public class EventCategory extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 240)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected EventCategory() {
    }

    public EventCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
