package com.jairomatias.eventix.reservation.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.reservation.entity.ReservationStatus;

public record ReservationDetailsView(
        Long id,
        String referenceCode,
        Long eventId,
        String eventTitle,
        LocalDateTime eventStartAt,
        String attendeeFirstName,
        String attendeeLastName,
        String attendeeFullName,
        String attendeeEmail,
        String attendeePhone,
        int quantity,
        ReservationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        String cancellationReason,
        String reservedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {
}
