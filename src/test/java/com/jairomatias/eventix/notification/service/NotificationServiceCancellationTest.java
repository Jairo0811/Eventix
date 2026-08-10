package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceCancellationTest {

    @Test
    void shouldIncludeCancellationReference() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendCancellation("customer@example.com", "RSV-900");

        verify(gateway).send(
                "customer@example.com",
                "Cancelación registrada | Eventix",
                "La operación RSV-900 ha sido cancelada."
        );
    }
}
