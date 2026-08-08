package com.jairomatias.eventix.ticket.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.ticket.entity.TicketStatus;

public record TicketListItem(
        Long id,
        String uniqueCode,
        String saleReference,
        Long eventId,
        String eventTitle,
        String attendeeName,
        String attendeeEmail,
        String ticketTypeName,
        TicketStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt) {
}
