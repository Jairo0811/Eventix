package com.jairomatias.eventix.role.entity;

public enum RoleName {
    ADMINISTRATOR("Administrador"),
    OPERATOR("Operador"),
    ORGANIZER("Organizador"),
    ACCESS_STAFF("Personal de acceso"),
    USER("Usuario");

    private final String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
