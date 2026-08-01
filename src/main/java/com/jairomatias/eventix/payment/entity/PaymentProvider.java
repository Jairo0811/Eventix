package com.jairomatias.eventix.payment.entity;

public enum PaymentProvider {
    STRIPE("Stripe"),
    PAYPAL("PayPal"),
    AZUL("Azul"),
    CARDNET("CardNET"),
    QIK("Qik"),
    BANK_TRANSFER("Transferencia bancaria");

    private final String displayName;

    PaymentProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
