package com.jairomatias.eventix.reservation.dto;

public record ReservationMetrics(
        Long eventId,
        String eventTitle,
        int capacity,
        int pendingSeats,
        int confirmedSeats,
        int availableSeats,
        int occupancyPercentage) {
}
