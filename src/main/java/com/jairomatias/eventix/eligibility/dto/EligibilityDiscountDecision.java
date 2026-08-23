package com.jairomatias.eventix.eligibility.dto;

import java.math.BigDecimal;
import java.util.Objects;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

public record EligibilityDiscountDecision(
        Long benefitId,
        EligibilityBenefitType benefitType,
        BigDecimal configuredValue,
        BigDecimal discountAmount) {

    public EligibilityDiscountDecision {
        Objects.requireNonNull(benefitId, "benefitId es obligatorio");
        Objects.requireNonNull(benefitType, "benefitType es obligatorio");
        Objects.requireNonNull(discountAmount, "discountAmount es obligatorio");
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("discountAmount debe ser mayor que cero.");
        }
    }
}
