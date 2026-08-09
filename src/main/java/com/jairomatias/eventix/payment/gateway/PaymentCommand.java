package com.jairomatias.eventix.payment.gateway;

import java.math.BigDecimal;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;

public record PaymentCommand(
        String saleReference,
        PaymentProvider provider,
        PaymentTransactionType transactionType,
        BigDecimal amount,
        String currency,
        SimulationOutcome simulationOutcome,
        String walletToken,
        String originalExternalReference) {

    public PaymentCommand(
            String saleReference,
            PaymentProvider provider,
            PaymentTransactionType transactionType,
            BigDecimal amount,
            String currency,
            SimulationOutcome simulationOutcome) {
        this(
                saleReference,
                provider,
                transactionType,
                amount,
                currency,
                simulationOutcome,
                null,
                null);
    }
}
