package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceCancellationRecipientTest {

    @Test
    void cancellationShouldGoToRequestedRecipient() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendCancellation("buyer@example.com", "RSV-40");

        verify(gateway).send(
                "buyer@example.com",
                "Cancelación registrada | Eventix",
                "La operación RSV-40 ha sido cancelada."
        );
    }
}
