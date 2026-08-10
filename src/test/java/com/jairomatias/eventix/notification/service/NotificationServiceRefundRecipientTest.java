package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceRefundRecipientTest {

    @Test
    void refundShouldGoToRequestedRecipient() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendRefundConfirmation("buyer@example.com", "REF-40");

        verify(gateway).send(
                "buyer@example.com",
                "Reembolso procesado | Eventix",
                "El reembolso asociado a REF-40 ha sido procesado."
        );
    }
}
