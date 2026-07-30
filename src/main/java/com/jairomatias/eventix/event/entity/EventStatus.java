package com.jairomatias.eventix.event.entity;

public enum EventStatus {
    DRAFT("Borrador"),
    PUBLISHED("Publicado"),
    CANCELLED("Cancelado"),
    FINISHED("Finalizado");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
