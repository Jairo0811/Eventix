package com.jairomatias.eventix.ticket.entity;

public enum TicketStatus {
    ACTIVE("Activa"),
    USED("Utilizada"),
    CANCELLED("Cancelada"),
    EXPIRED("Vencida");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
