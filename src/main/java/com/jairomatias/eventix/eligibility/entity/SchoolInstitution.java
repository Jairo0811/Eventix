package com.jairomatias.eventix.eligibility.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_institutions")
public class SchoolInstitution extends AuditableEntity {

    private static final int MAX_NAME_LENGTH = 180;
    private static final int MAX_CODE_LENGTH = 50;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, unique = true, length = MAX_CODE_LENGTH)
    private String code;

    @Column(nullable = false)
    private boolean active = true;

    protected SchoolInstitution() {
    }

    public SchoolInstitution(String name, String code) {
        update(name, code);
    }

    public void update(String name, String code) {
        String normalizedName = requireText(name, "El nombre de la institución es obligatorio.");
        String normalizedCode = requireText(code, "El código de la institución es obligatorio.")
                .toUpperCase();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre de la institución no puede superar 180 caracteres.");
        }
        if (normalizedCode.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "El código de la institución no puede superar 50 caracteres.");
        }
        this.name = normalizedName;
        this.code = normalizedCode;
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

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
