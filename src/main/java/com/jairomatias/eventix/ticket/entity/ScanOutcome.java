package com.jairomatias.eventix.ticket.entity;

public enum ScanOutcome {
    VALID("Entrada válida"),
    REENTRY("Reingreso autorizado"),
    DUPLICATE("Entrada ya utilizada"),
    CANCELLED("Entrada cancelada"),
    COUNTERFEIT("Entrada falsificada"),
    EXPIRED("Entrada vencida");

    private final String displayName;

    ScanOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean accepted() {
        return this == VALID || this == REENTRY;
    }
}
