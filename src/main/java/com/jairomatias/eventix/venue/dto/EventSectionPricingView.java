package com.jairomatias.eventix.venue.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.venue.entity.VenueSectionType;

public record EventSectionPricingView(
        Long id,
        Long sectionId,
        String sectionCode,
        String sectionName,
        VenueSectionType sectionType,
        Long ticketTypeId,
        String ticketTypeName,
        BigDecimal basePrice,
        BigDecimal priceOverride,
        BigDecimal effectivePrice) {
}
