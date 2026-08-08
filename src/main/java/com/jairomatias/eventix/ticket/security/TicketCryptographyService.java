package com.jairomatias.eventix.ticket.security;

import java.util.Optional;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;

public interface TicketCryptographyService {

    SignedTicketPayload sign(TicketSigningPayload payload);

    boolean verify(
            DigitalTicket ticket,
            ParsedTicketToken submittedToken);

    String createQrPayload(DigitalTicket ticket);

    Optional<ParsedTicketToken> parseQrPayload(String value);

    String sha256(String value);
}
