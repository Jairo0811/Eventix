package com.jairomatias.eventix.reporting.dto;

import java.math.BigDecimal;

public record MonthlyRevenueRow(
        int year,
        int month,
        String period,
        long sales,
        long ticketsSold,
        BigDecimal revenue) {
}
