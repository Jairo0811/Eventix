package com.jairomatias.eventix.payment.entity;

public enum PaymentTransactionType {
    CHARGE("Cobro"),
    REFUND("Reembolso");

    private final String displayName;

    PaymentTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
