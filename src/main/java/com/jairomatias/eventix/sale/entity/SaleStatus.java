package com.jairomatias.eventix.sale.entity;

public enum SaleStatus {
    PENDING("Pendiente"),
    PAID("Pagada"),
    PARTIALLY_REFUNDED("Reembolso parcial"),
    REFUNDED("Reembolsada"),
    CANCELLED("Cancelada");

    private final String displayName;

    SaleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean occupiesTicketTypeCapacity() {
        return this == PENDING || this == PAID || this == PARTIALLY_REFUNDED;
    }
}
