package com.jairomatias.eventix.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.promotion.entity.DiscountType;

public record CouponListItem(
        Long id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal value,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        boolean active,
        Integer totalUseLimit,
        int currentUses,
        Integer perUserLimit,
        BigDecimal minimumSubtotal) {
}
