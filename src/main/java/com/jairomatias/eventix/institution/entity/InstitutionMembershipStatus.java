package com.jairomatias.eventix.institution.entity;

public enum InstitutionMembershipStatus {
    ACTIVE("Activa"),
    SUSPENDED("Suspendida");

    private final String displayName;

    InstitutionMembershipStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
