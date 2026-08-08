package com.jairomatias.eventix.ticket.security;

public record SignedTicketPayload(
        String payloadHash,
        String signature,
        String keyId) {
}
