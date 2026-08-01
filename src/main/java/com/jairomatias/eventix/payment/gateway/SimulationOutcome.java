package com.jairomatias.eventix.payment.gateway;

public enum SimulationOutcome {
    APPROVE("Aprobar operación"),
    DECLINE("Rechazar operación");

    private final String displayName;

    SimulationOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
