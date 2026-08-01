package com.jairomatias.eventix.reservation.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.reservation.entity.ReservationStatus;

public record ReservationListItem(
        Long id,
        String referenceCode,
        Long eventId,
        String eventTitle,
        String attendeeFullName,
        String attendeeEmail,
        int quantity,
        ReservationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {
}
