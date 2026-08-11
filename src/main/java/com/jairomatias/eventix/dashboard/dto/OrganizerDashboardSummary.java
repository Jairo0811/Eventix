package com.jairomatias.eventix.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

import com.jairomatias.eventix.reporting.dto.MonthlyRevenueRow;

public record OrganizerDashboardSummary(
        long upcomingEvents,
        long publishedEvents,
        long ticketsSold,
        long activeReservations,
        BigDecimal occupancyRate,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        BigDecimal platformCommission,
        BigDecimal estimatedNet,
        long pendingSettlements,
        long paidSettlements,
        BigDecimal pendingSettlementNet,
        BigDecimal paidSettlementNet,
        List<OrganizerUpcomingEvent> upcoming,
        List<MonthlyRevenueRow> monthlyRevenue) {
}
