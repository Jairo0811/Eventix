package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;

public record EligibilityGroupView(
        Long id,
        Long eventId,
        String name,
        EligibilityGroupType groupType,
        Integer maxRelatedPeople,
        boolean active) {

    public static EligibilityGroupView from(EligibilityGroup group) {
        return new EligibilityGroupView(
                group.getId(),
                group.getEvent().getId(),
                group.getName(),
                group.getGroupType(),
                group.getMaxRelatedPeople(),
                group.isActive());
    }
}
