package com.jairomatias.eventix.payment.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;

/**
 * Facts from a previously authenticated provider event, not an API request.
 * Construct only after official bank signature and anti-replay verification.
 */
public record VerifiedReconciliationEvidence(
        PaymentProvider provider,
        String eventId,
        String payloadSha256,
        String saleReference,
        PaymentTransactionType type,
        String externalReference,
        PaymentStatus status,
        BigDecimal amount,
        String currency) {

    private static final Pattern SHA256 = Pattern.compile("[a-fA-F0-9]{64}");

    public VerifiedReconciliationEvidence {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(amount, "amount");
        if (!filled(eventId, 160) || !filled(saleReference, 24)
                || !filled(externalReference, 120)
                || payloadSha256 == null
                || !SHA256.matcher(payloadSha256).matches()
                || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() > 12
                || currency == null
                || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Datos mínimos de conciliación inválidos.");
        }
        payloadSha256 = payloadSha256.toLowerCase(Locale.ROOT);
    }

    private static boolean filled(String text, int length) {
        return text != null && !text.isBlank()
                && text.length() <= length && text.equals(text.trim());
    }
}
