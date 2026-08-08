package com.jairomatias.eventix.ticket.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.ticket.entity.ScanOutcome;

public record ScanResultView(
        ScanOutcome outcome,
        boolean accepted,
        String message,
        String ticketCode,
        String attendeeName,
        String eventTitle,
        String ticketTypeName,
        String zone,
        String seat,
        LocalDateTime occurredAt) {
}
