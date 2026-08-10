package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReferenceFormattingTest {

    @Test
    void shouldPreserveReferenceFormatting() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendPurchaseConfirmation("user@example.com", "SALE-2026-00001");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Compra confirmada | Eventix"),
                contains("SALE-2026-00001")
        );
    }
}
