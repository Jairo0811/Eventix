package com.jairomatias.eventix.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

class TicketDocumentServiceTest {

    @Test
    void createsReadablePdfAndPngQr() throws Exception {
        TicketFixture fixture = fixture("Concierto Eventix", "María Pérez",
                "VIP", "Teatro Nacional", "Santo Domingo");

        byte[] qr = fixture.service().createQrPng(fixture.ticket());
        byte[] pdf = fixture.service().createPdf(fixture.ticket());

        assertThat(qr).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(2_000);
    }

    @Test
    void preservesLongCriticalFieldsWithoutEllipsis() throws Exception {
        String eventName = "Festival Internacional de Tecnología Creativa y Experiencias Digitales Eventix 2026";
        String attendee = "Francis Jairo Matías Rosario de los Santos y Familia Invitada Especial";
        String ticketType = "Experiencia VIP Preferencial Ringside con Beneficios Especiales";
        String venue = "Arena Metropolitana de Espectáculos y Convenciones de Santo Domingo";
        String address = "Centro Olímpico Juan Pablo Duarte, Avenida Máximo Gómez, Santo Domingo, República Dominicana";
        TicketFixture fixture = fixture(eventName, attendee, ticketType, venue, address);

        byte[] pdf = fixture.service().createPdf(fixture.ticket());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(text).doesNotContain("...");
            assertThat(text).contains("Festival Internacional", "Francis Jairo", "Experiencia VIP", "Arena Metropolitana");
        }
    }

    private TicketFixture fixture(String eventName, String attendee, String ticketType,
            String venue, String address) {
        TicketCryptographyService cryptography = mock(TicketCryptographyService.class);
        TicketDocumentService service = new TicketDocumentService(cryptography);
        DigitalTicket ticket = mock(DigitalTicket.class);
        Event event = mock(Event.class);
        when(ticket.getEvent()).thenReturn(event);
        when(event.getTitle()).thenReturn(eventName);
        when(event.getStartAt()).thenReturn(LocalDateTime.of(2026, 8, 29, 19, 30));
        when(event.getVenue()).thenReturn(venue);
        when(event.getAddress()).thenReturn(address);
        when(ticket.getAttendeeName()).thenReturn(attendee);
        when(ticket.getTicketTypeName()).thenReturn(ticketType);
        when(ticket.getUniqueCode()).thenReturn("TKT-ABCDEFGH23456789JKLM");
        when(ticket.getAntiFraudCode()).thenReturn("AF-ABCDEFGH23456789JKLM");
        when(ticket.getStatus()).thenReturn(TicketStatus.ACTIVE);
        when(ticket.getSignatureKeyId()).thenReturn("test-key");
        when(ticket.getSignedPayloadHash()).thenReturn("a".repeat(64));
        when(cryptography.createQrPayload(ticket)).thenReturn(
                "EVX1.TKT-ABCDEFGH23456789JKLM.AF-ABCDEFGH23456789JKLM.signature");
        return new TicketFixture(service, ticket);
    }

    private record TicketFixture(TicketDocumentService service, DigitalTicket ticket) {
    }
}
