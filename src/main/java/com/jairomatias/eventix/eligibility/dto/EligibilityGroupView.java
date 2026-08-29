package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;

public record EligibilityGroupView(
        Long id,
        Long eventId,
        String name,
        EligibilityGroupType groupType,
        Integer maxRelatedPeople,
        Long schoolPromotionId,
        String schoolPromotionLabel,
        boolean active) {

    public static EligibilityGroupView from(EligibilityGroup group) {
        SchoolPromotion promotion = group.getSchoolPromotion();
        return new EligibilityGroupView(
                group.getId(),
                group.getEvent().getId(),
                group.getName(),
                group.getGroupType(),
                group.getMaxRelatedPeople(),
                promotion == null ? null : promotion.getId(),
                promotion == null ? null : promotion.getInstitution().getName()
                        + " · " + promotion.getName() + " " + promotion.getGraduationYear(),
                group.isActive());
    }
}
