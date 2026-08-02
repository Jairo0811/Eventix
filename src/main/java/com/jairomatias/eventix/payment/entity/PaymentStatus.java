package com.jairomatias.eventix.payment.entity;

public enum PaymentStatus {
    APPROVED("Aprobado"),
    DECLINED("Rechazado");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
