package com.jairomatias.eventix.ticket.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.ticket.entity.ScanOutcome;

public record ScanAttemptListItem(
        Long id,
        String ticketCode,
        String eventTitle,
        ScanOutcome outcome,
        LocalDateTime occurredAt,
        String scannedBy,
        String deviceIdentifier,
        String ipAddress,
        boolean firstAccess,
        boolean duplicateAttempt,
        String notes) {
}
