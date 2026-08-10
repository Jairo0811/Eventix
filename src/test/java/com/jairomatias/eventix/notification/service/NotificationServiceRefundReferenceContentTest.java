package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceRefundReferenceContentTest {

    @Test
    void refundShouldContainExactReference() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendRefundConfirmation("user@example.com", "REFUND-2026-999");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Reembolso procesado | Eventix"),
                contains("REFUND-2026-999")
        );
    }
}
