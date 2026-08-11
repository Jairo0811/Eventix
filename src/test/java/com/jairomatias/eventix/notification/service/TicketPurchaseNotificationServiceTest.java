package com.jairomatias.eventix.notification.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class TicketPurchaseNotificationServiceTest {

    @Test
    void sendsGeneratedTicketsThroughAttachmentCapablePort() {
        EmailAttachmentGateway gateway = mock(EmailAttachmentGateway.class);
        TicketEmailAttachmentService attachmentService = mock(
                TicketEmailAttachmentService.class);
        EmailAttachment attachment = new EmailAttachment(
                "eventix-TKT-1.pdf",
                "application/pdf",
                new byte[] {1});
        when(attachmentService.createForSale(20L, "SALE-100"))
                .thenReturn(List.of(attachment));
        TicketPurchaseNotificationService service =
                new TicketPurchaseNotificationService(
                        gateway,
                        attachmentService);

        service.sendPurchaseConfirmation(
                20L,
                "buyer@example.com",
                "SALE-100");

        verify(gateway).sendWithAttachments(
                "buyer@example.com",
                "Compra confirmada | Eventix",
                "Tu compra SALE-100 ha sido procesada correctamente. "
                        + "Tus boletas digitales están adjuntas a este correo.",
                List.of(attachment));
    }
}
