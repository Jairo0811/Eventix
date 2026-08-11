package com.jairomatias.eventix.settlement.entity;

public enum SettlementLineType {
    SALE("Venta"),
    REFUND("Reembolso");

    private final String displayName;

    SettlementLineType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
