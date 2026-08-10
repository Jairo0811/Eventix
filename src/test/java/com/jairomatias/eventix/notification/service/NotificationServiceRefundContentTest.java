package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceRefundContentTest {

    @Test
    void refundMessageShouldConfirmProcessing() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendRefundConfirmation("buyer@example.com", "REF-300");

        verify(gateway).send(
                eq("buyer@example.com"),
                eq("Reembolso procesado | Eventix"),
                contains("procesado")
        );
    }
}
