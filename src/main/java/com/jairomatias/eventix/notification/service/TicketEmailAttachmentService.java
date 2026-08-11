package com.jairomatias.eventix.notification.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.service.TicketDocumentService;

@Service
public class TicketEmailAttachmentService {

    private final DigitalTicketRepository ticketRepository;
    private final TicketDocumentService ticketDocumentService;
    private final NotificationProperties notificationProperties;

    public TicketEmailAttachmentService(
            DigitalTicketRepository ticketRepository,
            TicketDocumentService ticketDocumentService,
            NotificationProperties notificationProperties) {
        this.ticketRepository = ticketRepository;
        this.ticketDocumentService = ticketDocumentService;
        this.notificationProperties = notificationProperties;
    }

    @Transactional(readOnly = true)
    public List<EmailAttachment> createForSale(
            Long saleId,
            String saleReference) {
        if (!notificationProperties.enabled()) {
            return List.of();
        }
        List<DigitalTicket> tickets = ticketRepository
                .findAllBySale_IdOrderBySequenceNumberAsc(saleId);
        if (tickets.isEmpty()) {
            return List.of();
        }
        if (tickets.size() == 1) {
            DigitalTicket ticket = tickets.getFirst();
            return List.of(new EmailAttachment(
                    "eventix-" + ticket.getUniqueCode() + ".pdf",
                    "application/pdf",
                    ticketDocumentService.createPdf(ticket)));
        }
        return List.of(new EmailAttachment(
                "eventix-boletas-" + safeReference(saleReference) + ".zip",
                "application/zip",
                createZip(tickets)));
    }

    private byte[] createZip(List<DigitalTicket> tickets) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            for (DigitalTicket ticket : tickets) {
                zip.putNextEntry(new ZipEntry(
                        "eventix-" + ticket.getSequenceNumber()
                                + '-' + ticket.getUniqueCode() + ".pdf"));
                zip.write(ticketDocumentService.createPdf(ticket));
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo preparar el paquete de boletas.",
                    exception);
        }
    }

    private String safeReference(String saleReference) {
        if (saleReference == null || saleReference.isBlank()) {
            return "compra";
        }
        return saleReference.replaceAll("[^A-Za-z0-9_-]", "-");
    }
}
