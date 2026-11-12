package com.jairomatias.eventix.institution.dto;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitutionStatus;
import com.jairomatias.eventix.institution.entity.InstitutionMembership;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipStatus;

public record InstitutionMembershipView(
        Long membershipId,
        Long institutionId,
        String institutionName,
        String institutionCode,
        SchoolInstitutionStatus institutionStatus,
        InstitutionMembershipRole role,
        InstitutionMembershipStatus membershipStatus) {

    public static InstitutionMembershipView from(InstitutionMembership membership) {
        return new InstitutionMembershipView(
                membership.getId(),
                membership.getInstitution().getId(),
                membership.getInstitution().getName(),
                membership.getInstitution().getCode(),
                membership.getInstitution().getStatus(),
                membership.getRole(),
                membership.getStatus());
    }
}
