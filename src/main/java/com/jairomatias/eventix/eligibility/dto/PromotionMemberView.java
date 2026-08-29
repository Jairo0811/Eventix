package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;

public record PromotionMemberView(
        Long id,
        String fullName,
        String studentCode,
        String nationalIdLast4,
        String sourceReference,
        boolean active) {

    public static PromotionMemberView from(PromotionMember member) {
        return new PromotionMemberView(
                member.getId(),
                member.getFullName(),
                member.getStudentCode(),
                member.getNationalIdLast4(),
                member.getSourceReference(),
                member.isActive());
    }
}
