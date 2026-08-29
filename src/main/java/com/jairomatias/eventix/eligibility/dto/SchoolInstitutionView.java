package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;

public record SchoolInstitutionView(Long id, String name, String code, boolean active) {

    public static SchoolInstitutionView from(SchoolInstitution institution) {
        return new SchoolInstitutionView(
                institution.getId(),
                institution.getName(),
                institution.getCode(),
                institution.isActive());
    }
}
