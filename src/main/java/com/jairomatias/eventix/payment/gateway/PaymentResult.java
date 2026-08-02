package com.jairomatias.eventix.payment.gateway;

import com.jairomatias.eventix.payment.entity.PaymentStatus;

public record PaymentResult(
        PaymentStatus status,
        String externalReference,
        String message) {
}
