package com.jairomatias.eventix.eligibility.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.eligibility.entity.EligibilityVerification;
import com.jairomatias.eventix.eligibility.entity.VerificationMethod;
import com.jairomatias.eventix.eligibility.entity.VerificationStatus;

public record SchoolVerificationView(
        Long id,
        Long userId,
        String userName,
        String institutionName,
        String promotionName,
        int graduationYear,
        String memberName,
        VerificationStatus status,
        VerificationMethod method,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime verifiedAt) {

    public static SchoolVerificationView from(EligibilityVerification verification) {
        var member = verification.getPromotionMember();
        var promotion = member.getPromotion();
        return new SchoolVerificationView(
                verification.getId(),
                verification.getUser().getId(),
                verification.getUser().getFullName(),
                promotion.getInstitution().getName(),
                promotion.getName(),
                promotion.getGraduationYear(),
                member.getFullName(),
                verification.getStatus(),
                verification.getVerificationMethod(),
                verification.getReason(),
                verification.getCreatedAt(),
                verification.getVerifiedAt());
    }
}
