package com.jairomatias.eventix.ticket.dto;

public record TicketSummary(
        long total,
        long active,
        long used,
        long cancelled,
        long expired) {
}
