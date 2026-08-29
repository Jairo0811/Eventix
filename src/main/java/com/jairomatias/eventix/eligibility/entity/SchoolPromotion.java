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

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MIN_GRADUATION_YEAR = 1900;
    private static final int MAX_GRADUATION_YEAR = 2200;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private SchoolInstitution institution;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "graduation_year", nullable = false)
    private int graduationYear;

    @Column(nullable = false)
    private boolean active = true;

    protected SchoolPromotion() {
    }

    public SchoolPromotion(SchoolInstitution institution, String name, int graduationYear) {
        if (institution == null) {
            throw new IllegalArgumentException("La institución de la promoción es obligatoria.");
        }
        this.institution = institution;
        update(name, graduationYear);
    }

    public void update(String name, int graduationYear) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la promoción es obligatorio.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre de la promoción no puede superar 120 caracteres.");
        }
        if (graduationYear < MIN_GRADUATION_YEAR || graduationYear > MAX_GRADUATION_YEAR) {
            throw new IllegalArgumentException("El año de graduación debe estar entre 1900 y 2200.");
        }
        this.name = normalizedName;
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
