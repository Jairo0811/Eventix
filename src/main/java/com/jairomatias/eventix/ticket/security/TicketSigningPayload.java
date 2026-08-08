package com.jairomatias.eventix.ticket.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;

public record TicketSigningPayload(
        String uniqueCode,
        String saleReference,
        Long eventId,
        String attendeeEmail,
        String ticketTypeName,
        int sequenceNumber,
        LocalDateTime issuedAt,
        String antiFraudCode) {

    public static TicketSigningPayload from(DigitalTicket ticket) {
        return new TicketSigningPayload(
                ticket.getUniqueCode(),
                ticket.getSale().getReferenceCode(),
                ticket.getEvent().getId(),
                ticket.getAttendeeEmail(),
                ticket.getTicketTypeName(),
                ticket.getSequenceNumber(),
                ticket.getIssuedAt(),
                ticket.getAntiFraudCode());
    }

    public String canonicalValue() {
        return String.join(
                "|",
                "EVX1",
                uniqueCode,
                saleReference,
                String.valueOf(eventId),
                attendeeEmail.trim().toLowerCase(Locale.ROOT),
                ticketTypeName.trim(),
                String.valueOf(sequenceNumber),
                issuedAt.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                antiFraudCode);
    }
}
