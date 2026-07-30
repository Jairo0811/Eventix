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
        long finishedEvents) {
}
