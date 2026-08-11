package com.jairomatias.eventix.dashboard.dto;

import java.math.BigDecimal;

public record OrganizerDashboardMetrics(
        long upcomingEvents,
        long publishedEvents,
        long publishedCapacity,
        long ticketsSold,
        long activeReservations,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        BigDecimal platformCommission,
        BigDecimal estimatedNet,
        long pendingSettlements,
        long paidSettlements,
        BigDecimal pendingSettlementNet,
        BigDecimal paidSettlementNet) {
}
