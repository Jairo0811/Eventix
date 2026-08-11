package com.jairomatias.eventix.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrganizerUpcomingEvent(
        Long id,
        String title,
        LocalDateTime startAt,
        int capacity,
        long ticketsSold,
        long activeReservations,
        BigDecimal occupancyRate,
        BigDecimal paidRevenue,
        BigDecimal estimatedNet) {
}
