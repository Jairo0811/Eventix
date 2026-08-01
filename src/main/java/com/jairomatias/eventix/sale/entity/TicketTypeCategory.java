package com.jairomatias.eventix.sale.entity;

public enum TicketTypeCategory {
    GENERAL("General"),
    VIP("VIP"),
    PREFERENTIAL("Preferencial"),
    STUDENT("Estudiante"),
    COMPLIMENTARY("Cortesía"),
    CUSTOM("Personalizado");

    private final String displayName;

    TicketTypeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
