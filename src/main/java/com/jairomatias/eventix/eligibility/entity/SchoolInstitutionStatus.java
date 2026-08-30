package com.jairomatias.eventix.eligibility.entity;

public enum SchoolInstitutionStatus {
    PENDING_VERIFICATION("Pendiente de verificación"),
    ACTIVE("Activa"),
    REJECTED("Rechazada"),
    SUSPENDED("Suspendida");

    private final String displayName;

    SchoolInstitutionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
