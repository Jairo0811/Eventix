package com.jairomatias.eventix.venue.dto;

import com.jairomatias.eventix.venue.entity.VenueSectionType;

public record VenueSectionPricingOption(
        Long id,
        String code,
        String name,
        VenueSectionType sectionType) {
}
