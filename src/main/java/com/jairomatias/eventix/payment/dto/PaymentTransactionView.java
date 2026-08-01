package com.jairomatias.eventix.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;

public record PaymentTransactionView(
        Long id,
        String transactionReference,
        PaymentProvider provider,
        PaymentTransactionType transactionType,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String externalReference,
        String responseMessage,
        LocalDateTime processedAt,
        String processedByName) {
}
