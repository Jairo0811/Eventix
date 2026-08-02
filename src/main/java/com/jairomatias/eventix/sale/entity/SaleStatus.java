package com.jairomatias.eventix.sale.entity;

public enum SaleStatus {
    PENDING("Pendiente"),
    PAID("Pagada"),
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
        return this == PENDING || this == PAID;
    }
}
