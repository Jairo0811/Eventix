package com.jairomatias.eventix.notification.service;

import org.springframework.stereotype.Service;

@Service
public class TicketPurchaseNotificationService {

    private final EmailAttachmentGateway emailGateway;
    private final TicketEmailAttachmentService attachmentService;

    public TicketPurchaseNotificationService(
            EmailAttachmentGateway emailGateway,
            TicketEmailAttachmentService attachmentService) {
        this.emailGateway = emailGateway;
        this.attachmentService = attachmentService;
    }

    public void sendPurchaseConfirmation(
            Long saleId,
            String recipient,
            String saleCode) {
        emailGateway.sendWithAttachments(
                recipient,
                "Compra confirmada | Eventix",
                "Tu compra " + saleCode
                        + " ha sido procesada correctamente. "
                        + "Tus boletas digitales están adjuntas a este correo.",
                attachmentService.createForSale(saleId, saleCode));
    }
}
