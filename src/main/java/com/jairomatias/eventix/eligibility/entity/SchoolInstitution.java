package com.jairomatias.eventix.eligibility.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_institutions")
public class SchoolInstitution extends AuditableEntity {

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private boolean active = true;

    protected SchoolInstitution() {
    }

    public SchoolInstitution(String name, String code) {
        update(name, code);
    }

    public void update(String name, String code) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la institución es obligatorio.");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de la institución es obligatorio.");
        }
        this.name = name.trim();
        this.code = code.trim().toUpperCase();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
