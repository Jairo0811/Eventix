package com.jairomatias.eventix.eligibility.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SchoolInstitutionStatus status = SchoolInstitutionStatus.ACTIVE;

    protected SchoolInstitution() {
    }

    public SchoolInstitution(String name, String code) {
        update(name, code);
    }

    public static SchoolInstitution pendingRegistration(String name, String code) {
        SchoolInstitution institution = new SchoolInstitution(name, code);
        institution.active = false;
        institution.status = SchoolInstitutionStatus.PENDING_VERIFICATION;
        return institution;
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
        this.status = SchoolInstitutionStatus.ACTIVE;
    }

    public void deactivate() {
        suspend();
    }

    public void approve() {
        activate();
    }

    public void reject() {
        this.active = false;
        this.status = SchoolInstitutionStatus.REJECTED;
    }

    public void suspend() {
        this.active = false;
        this.status = SchoolInstitutionStatus.SUSPENDED;
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

    public SchoolInstitutionStatus getStatus() {
        return status;
    }

    public boolean isOperational() {
        return active && status == SchoolInstitutionStatus.ACTIVE;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
