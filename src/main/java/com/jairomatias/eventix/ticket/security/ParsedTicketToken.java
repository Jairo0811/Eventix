package com.jairomatias.eventix.ticket.security;

public record ParsedTicketToken(
        String uniqueCode,
        String antiFraudCode,
        String signature) {
}
