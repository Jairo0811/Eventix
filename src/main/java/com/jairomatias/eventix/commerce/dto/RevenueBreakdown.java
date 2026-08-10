package com.jairomatias.eventix.commerce.dto;

import java.math.BigDecimal;

public record RevenueBreakdown(
        BigDecimal grossRevenue,
        BigDecimal platformRevenue,
        BigDecimal organizerNetRevenue,
        BigDecimal refundedRevenue,
        BigDecimal effectivePlatformRate) {
}
