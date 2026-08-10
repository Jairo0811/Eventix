package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceRefundTest {

    @Test
    void shouldSendRefundNotification() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendRefundConfirmation("buyer@example.com", "PAY-500");

        verify(gateway).send(
                "buyer@example.com",
                "Reembolso procesado | Eventix",
                "El reembolso asociado a PAY-500 ha sido procesado."
        );
    }
}
