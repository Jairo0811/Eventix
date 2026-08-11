package com.jairomatias.eventix.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.service.TicketDocumentService;

class TicketEmailAttachmentServiceTest {

    @Test
    void attachesSingleTicketAsPdf() {
        DigitalTicketRepository repository = mock(
                DigitalTicketRepository.class);
        TicketDocumentService documentService = mock(
                TicketDocumentService.class);
        DigitalTicket ticket = mock(DigitalTicket.class);
        when(ticket.getUniqueCode()).thenReturn("TKT-100");
        when(repository.findAllBySale_IdOrderBySequenceNumberAsc(20L))
                .thenReturn(List.of(ticket));
        when(documentService.createPdf(ticket))
                .thenReturn("pdf".getBytes(StandardCharsets.UTF_8));
        TicketEmailAttachmentService service =
                new TicketEmailAttachmentService(
                        repository,
                        documentService,
                        new NotificationProperties(true, "from@example.com"));

        List<EmailAttachment> attachments = service.createForSale(
                20L,
                "SALE-100");

        assertThat(attachments).singleElement().satisfies(attachment -> {
            assertThat(attachment.filename()).isEqualTo("eventix-TKT-100.pdf");
            assertThat(attachment.contentType()).isEqualTo("application/pdf");
            assertThat(attachment.content())
                    .isEqualTo("pdf".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void packagesMultipleTicketsInSingleZipAttachment() {
        DigitalTicketRepository repository = mock(
                DigitalTicketRepository.class);
        TicketDocumentService documentService = mock(
                TicketDocumentService.class);
        DigitalTicket first = ticket("TKT-1", 1);
        DigitalTicket second = ticket("TKT-2", 2);
        when(repository.findAllBySale_IdOrderBySequenceNumberAsc(21L))
                .thenReturn(List.of(first, second));
        when(documentService.createPdf(first)).thenReturn(new byte[] {1});
        when(documentService.createPdf(second)).thenReturn(new byte[] {2});
        TicketEmailAttachmentService service =
                new TicketEmailAttachmentService(
                        repository,
                        documentService,
                        new NotificationProperties(true, "from@example.com"));

        EmailAttachment attachment = service.createForSale(
                21L,
                "SALE/unsafe").getFirst();

        assertThat(attachment.filename())
                .isEqualTo("eventix-boletas-SALE-unsafe.zip");
        assertThat(attachment.contentType()).isEqualTo("application/zip");
        assertThat(attachment.content()).startsWith(0x50, 0x4b);
    }

    private DigitalTicket ticket(String code, int sequence) {
        DigitalTicket ticket = mock(DigitalTicket.class);
        when(ticket.getUniqueCode()).thenReturn(code);
        when(ticket.getSequenceNumber()).thenReturn(sequence);
        return ticket;
    }
}
