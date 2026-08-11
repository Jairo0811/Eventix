package com.jairomatias.eventix.settlement.entity;

public enum SettlementStatus {
    PENDING("Pendiente"),
    PROCESSING("En procesamiento"),
    PAID("Pagada"),
    FAILED("Fallida"),
    CANCELLED("Cancelada");

    private final String displayName;

    SettlementStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
