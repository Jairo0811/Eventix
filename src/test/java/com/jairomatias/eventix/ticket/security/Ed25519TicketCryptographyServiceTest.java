package com.jairomatias.eventix.ticket.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;

class Ed25519TicketCryptographyServiceTest {

    private static final LocalDateTime ISSUED_AT =
            LocalDateTime.of(2026, 8, 8, 18, 30);

    private Ed25519TicketCryptographyService service;

    @BeforeEach
    void setUp() {
        TicketingProperties properties = new TicketingProperties();
        properties.setSigningKeyId("test-key");
        service = new Ed25519TicketCryptographyService(properties);
    }

    @Test
    void signsAndVerifiesCanonicalTicketPayload() {
        TicketSigningPayload payload = payload();
        SignedTicketPayload signed = service.sign(payload);
        DigitalTicket ticket = ticket(payload, signed);
        ParsedTicketToken token = service.parseQrPayload(
                        service.createQrPayload(ticket))
                .orElseThrow();

        assertThat(service.verify(ticket, token)).isTrue();
        assertThat(signed.payloadHash()).hasSize(64);
        assertThat(signed.keyId()).isEqualTo("test-key");
    }

    @Test
    void rejectsQrWhenAntiFraudCodeWasAltered() {
        TicketSigningPayload payload = payload();
        SignedTicketPayload signed = service.sign(payload);
        DigitalTicket ticket = ticket(payload, signed);
        ParsedTicketToken altered = new ParsedTicketToken(
                payload.uniqueCode(),
                "AF-ALTERED",
                signed.signature());

        assertThat(service.verify(ticket, altered)).isFalse();
    }

    @Test
    void rejectsMalformedOrOversizedQrPayload() {
        assertThat(service.parseQrPayload("not-a-ticket")).isEmpty();
        assertThat(service.parseQrPayload("x".repeat(513))).isEmpty();
    }

    private TicketSigningPayload payload() {
        return new TicketSigningPayload(
                "TKT-ABCDEFGH23456789JKLM",
                "SAL-ABCDEFGH2345",
                8L,
                "asistente@example.com",
                "VIP",
                1,
                ISSUED_AT,
                "AF-ABCDEFGH23456789JKLM");
    }

    private DigitalTicket ticket(
            TicketSigningPayload payload,
            SignedTicketPayload signed) {
        DigitalTicket ticket = mock(DigitalTicket.class);
        Sale sale = mock(Sale.class);
        Event event = mock(Event.class);
        when(ticket.getUniqueCode()).thenReturn(payload.uniqueCode());
        when(ticket.getSale()).thenReturn(sale);
        when(sale.getReferenceCode()).thenReturn(payload.saleReference());
        when(ticket.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(payload.eventId());
        when(ticket.getAttendeeEmail()).thenReturn(payload.attendeeEmail());
        when(ticket.getTicketTypeName()).thenReturn(payload.ticketTypeName());
        when(ticket.getSequenceNumber()).thenReturn(payload.sequenceNumber());
        when(ticket.getIssuedAt()).thenReturn(payload.issuedAt());
        when(ticket.getAntiFraudCode()).thenReturn(payload.antiFraudCode());
        when(ticket.getSignedPayloadHash()).thenReturn(signed.payloadHash());
        when(ticket.getDigitalSignature()).thenReturn(signed.signature());
        when(ticket.getSignatureKeyId()).thenReturn(signed.keyId());
        return ticket;
    }
}
