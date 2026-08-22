package com.jairomatias.eventix.eligibility.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;

public record EligibilityMembershipView(
        Long id,
        Long userId,
        String fullName,
        String email,
        EligibilityMembershipStatus status,
        boolean active,
        LocalDateTime verifiedAt,
        String sponsorName) {

    public static EligibilityMembershipView from(EligibilityMembership membership) {
        return new EligibilityMembershipView(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getFullName(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.isActive(),
                membership.getVerifiedAt(),
                membership.getSponsorUser() == null ? null : membership.getSponsorUser().getFullName());
    }
}
