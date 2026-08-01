package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;

public record SalesSummary(
        long totalSales,
        long pendingSales,
        long paidSales,
        long refundedSales,
        long cancelledSales,
        BigDecimal grossRevenue,
        BigDecimal refundedAmount,
        BigDecimal netRevenue) {
}
