package com.jairomatias.eventix.institution.dto;

import com.jairomatias.eventix.institution.entity.InstitutionMembership;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipStatus;

public record InstitutionMemberView(
        Long membershipId,
        Long userId,
        String fullName,
        String email,
        InstitutionMembershipRole role,
        InstitutionMembershipStatus status) {

    public static InstitutionMemberView from(InstitutionMembership membership) {
        return new InstitutionMemberView(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getFullName(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getStatus());
    }
}
