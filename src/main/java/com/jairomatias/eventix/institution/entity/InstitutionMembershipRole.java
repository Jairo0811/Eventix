package com.jairomatias.eventix.institution.entity;

public enum InstitutionMembershipRole {
    OWNER("Propietario"),
    ADMIN("Administrador institucional"),
    EVENT_MANAGER("Gestor de eventos"),
    ROSTER_MANAGER("Gestor de padrones"),
    FINANCE("Finanzas");

    private final String displayName;

    InstitutionMembershipRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canManageTeam() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canManageEvents() {
        return this == OWNER || this == ADMIN || this == EVENT_MANAGER;
    }

    public boolean canManageRoster() {
        return this == OWNER || this == ADMIN || this == ROSTER_MANAGER;
    }
}
