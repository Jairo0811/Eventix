package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReferenceTest {

    @Test
    void shouldIncludeRefundReference() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendRefundConfirmation("customer@example.com", "REF-900");

        verify(gateway).send(
                "customer@example.com",
                "Reembolso procesado | Eventix",
                "El reembolso asociado a REF-900 ha sido procesado."
        );
    }
}
