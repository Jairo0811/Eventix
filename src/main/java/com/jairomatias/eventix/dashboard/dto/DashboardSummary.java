package com.jairomatias.eventix.dashboard.dto;

public record DashboardSummary(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long lockedUsers,
        long totalEvents,
        long draftEvents,
        long publishedEvents,
        long cancelledEvents,
        long finishedEvents,
        long totalReservations,
        long pendingReservations,
        long confirmedReservations,
        long cancelledReservations,
        long expiredReservations,
        long totalSales,
        long pendingSales,
        long paidSales,
        long refundedSales,
        long cancelledSales) {
}
