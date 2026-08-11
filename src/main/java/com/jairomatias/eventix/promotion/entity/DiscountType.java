package com.jairomatias.eventix.promotion.entity;

public enum DiscountType {
    PERCENTAGE("Porcentaje"),
    FIXED_AMOUNT("Monto fijo");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
