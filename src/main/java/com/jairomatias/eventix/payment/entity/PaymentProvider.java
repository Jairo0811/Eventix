package com.jairomatias.eventix.payment.entity;

public enum PaymentProvider {
    STRIPE("Stripe", false),
    PAYPAL("PayPal", false),
    AZUL("Azul", false),
    CARDNET("CardNET", false),
    QIK("Qik", false),
    BANK_TRANSFER("Transferencia bancaria", false),
    APPLE_PAY("Apple Pay", true),
    GOOGLE_PAY("Google Pay", true);

    private final String displayName;
    private final boolean digitalWallet;

    PaymentProvider(String displayName, boolean digitalWallet) {
        this.displayName = displayName;
        this.digitalWallet = digitalWallet;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDigitalWallet() {
        return digitalWallet;
    }
}
