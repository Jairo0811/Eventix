package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;

public record PromotionMemberView(
        Long id,
        String fullName,
        String studentCode,
        String sourceReference,
        boolean active) {

    public static PromotionMemberView from(PromotionMember member) {
        return new PromotionMemberView(
                member.getId(),
                member.getFullName(),
                member.getStudentCode(),
                member.getSourceReference(),
                member.isActive());
    }
}
