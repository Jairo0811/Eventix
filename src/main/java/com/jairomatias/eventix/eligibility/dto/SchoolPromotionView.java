package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;

public record SchoolPromotionView(
        Long id,
        Long institutionId,
        String institutionName,
        String institutionCode,
        String name,
        int graduationYear,
        boolean active) {

    public static SchoolPromotionView from(SchoolPromotion promotion) {
        return new SchoolPromotionView(
                promotion.getId(),
                promotion.getInstitution().getId(),
                promotion.getInstitution().getName(),
                promotion.getInstitution().getCode(),
                promotion.getName(),
                promotion.getGraduationYear(),
                promotion.isActive());
    }

    public String label() {
        return institutionName + " · " + name + " " + graduationYear;
    }
}
