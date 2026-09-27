package com.jairomatias.eventix.eligibility.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

public record SchoolAlumniCheckoutView(
        Long promotionId,
        String institutionName,
        String promotionName,
        int graduationYear,
        EligibilityBenefitType discountType,
        BigDecimal discountValue,
        boolean verified) {

    public String promotionLabel() {
        return promotionName + " " + graduationYear;
    }
}
