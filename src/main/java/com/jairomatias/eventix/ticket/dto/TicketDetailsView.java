package com.jairomatias.eventix.ticket.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.ticket.entity.TicketStatus;

public record TicketDetailsView(
        Long id,
        String uniqueCode,
        Long saleId,
        String saleReference,
        Long eventId,
        String eventTitle,
        LocalDateTime eventStartAt,
        LocalDateTime eventEndAt,
        String venue,
        String address,
        String organizerName,
        String attendeeName,
        String attendeeEmail,
        String ticketTypeName,
        String zone,
        String seat,
        TicketStatus status,
        String antiFraudCode,
        String signatureKeyId,
        String signatureFingerprint,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        LocalDateTime cancelledAt,
        String cancellationReason,
        boolean googleWalletAvailable,
        boolean appleWalletAvailable) {
}
