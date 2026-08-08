package com.jairomatias.eventix.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

class TicketDocumentServiceTest {

    @Test
    void createsReadablePdfAndPngQr() {
        TicketCryptographyService cryptography =
                mock(TicketCryptographyService.class);
        TicketDocumentService service = new TicketDocumentService(
                cryptography);
        DigitalTicket ticket = mock(DigitalTicket.class);
        Event event = mock(Event.class);
        when(ticket.getEvent()).thenReturn(event);
        when(event.getTitle()).thenReturn("Concierto Eventix");
        when(event.getStartAt()).thenReturn(
                LocalDateTime.of(2026, 8, 8, 19, 0));
        when(event.getVenue()).thenReturn("Teatro Nacional");
        when(event.getAddress()).thenReturn("Santo Domingo");
        when(ticket.getAttendeeName()).thenReturn("María Pérez");
        when(ticket.getTicketTypeName()).thenReturn("VIP");
        when(ticket.getUniqueCode())
                .thenReturn("TKT-ABCDEFGH23456789JKLM");
        when(ticket.getAntiFraudCode())
                .thenReturn("AF-ABCDEFGH23456789JKLM");
        when(ticket.getStatus()).thenReturn(TicketStatus.ACTIVE);
        when(ticket.getSignatureKeyId()).thenReturn("test-key");
        when(ticket.getSignedPayloadHash()).thenReturn("a".repeat(64));
        when(cryptography.createQrPayload(ticket)).thenReturn(
                "EVX1.TKT-ABCDEFGH23456789JKLM."
                + "AF-ABCDEFGH23456789JKLM.signature");

        byte[] qr = service.createQrPng(ticket);
        byte[] pdf = service.createPdf(ticket);

        assertThat(qr).startsWith(
                (byte) 0x89,
                (byte) 0x50,
                (byte) 0x4E,
                (byte) 0x47);
        assertThat(new String(
                pdf,
                0,
                4,
                StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(2_000);
    }
}
