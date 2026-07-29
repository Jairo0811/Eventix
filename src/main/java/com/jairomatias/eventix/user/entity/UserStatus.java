package com.jairomatias.eventix.user.entity;

public enum UserStatus {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    LOCKED("Bloqueado");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

