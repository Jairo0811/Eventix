package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServicePurchaseContentTest {

    @Test
    void purchaseMessageShouldConfirmProcessing() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendPurchaseConfirmation("buyer@example.com", "SALE-301");

        verify(gateway).send(
                eq("buyer@example.com"),
                eq("Compra confirmada | Eventix"),
                contains("procesada")
        );
    }
}
