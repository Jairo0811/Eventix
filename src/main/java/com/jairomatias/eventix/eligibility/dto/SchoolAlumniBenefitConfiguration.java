package com.jairomatias.eventix.eligibility.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

public record SchoolAlumniBenefitConfiguration(
        boolean enabled,
        Long schoolPromotionId,
        EligibilityBenefitType discountType,
        BigDecimal discountValue) {

    public static SchoolAlumniBenefitConfiguration disabled() {
        return new SchoolAlumniBenefitConfiguration(
                false,
                null,
                EligibilityBenefitType.PERCENTAGE_DISCOUNT,
                null);
    }
}
