package com.jairomatias.eventix.eligibility.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

public record EligibilityBenefitView(
        Long id,
        EligibilityBenefitType benefitType,
        BigDecimal discountValue,
        Integer maxTicketsPerPurchase,
        Integer reservedInventory,
        Long ticketTypeId,
        String ticketTypeName,
        LocalDateTime earlyAccessAt,
        boolean active) {

    public static EligibilityBenefitView from(EligibilityBenefit benefit) {
        return new EligibilityBenefitView(
                benefit.getId(),
                benefit.getBenefitType(),
                benefit.getDiscountValue(),
                benefit.getMaxTicketsPerPurchase(),
                benefit.getReservedInventory(),
                benefit.getTicketType() == null ? null : benefit.getTicketType().getId(),
                benefit.getTicketType() == null ? null : benefit.getTicketType().getName(),
                benefit.getEarlyAccessAt(),
                benefit.isActive());
    }
}
