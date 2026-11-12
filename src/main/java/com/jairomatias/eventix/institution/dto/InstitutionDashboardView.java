package com.jairomatias.eventix.institution.dto;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitutionStatus;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;

public record InstitutionDashboardView(
        Long institutionId,
        String name,
        String code,
        SchoolInstitutionStatus status,
        InstitutionMembershipRole currentRole,
        boolean operational,
        boolean canManageTeam,
        boolean canManageEvents,
        boolean canManageRoster) {
}
