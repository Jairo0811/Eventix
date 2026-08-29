package com.jairomatias.eventix.eligibility.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_promotions")
public class SchoolPromotion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private SchoolInstitution institution;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "graduation_year", nullable = false)
    private int graduationYear;

    @Column(nullable = false)
    private boolean active = true;

    protected SchoolPromotion() {
    }

    public SchoolPromotion(SchoolInstitution institution, String name, int graduationYear) {
        this.institution = institution;
        update(name, graduationYear);
    }

    public void update(String name, int graduationYear) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la promoción es obligatorio.");
        }
        if (graduationYear < 1900 || graduationYear > 2200) {
            throw new IllegalArgumentException("El año de graduación debe estar entre 1900 y 2200.");
        }
        this.name = name.trim();
        this.graduationYear = graduationYear;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public SchoolInstitution getInstitution() {
        return institution;
    }

    public String getName() {
        return name;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public boolean isActive() {
        return active;
    }
}
