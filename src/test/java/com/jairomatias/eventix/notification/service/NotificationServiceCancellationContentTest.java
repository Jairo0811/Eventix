package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceCancellationContentTest {

    @Test
    void cancellationMessageShouldConfirmCancellation() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendCancellation("buyer@example.com", "RSV-300");

        verify(gateway).send(
                eq("buyer@example.com"),
                eq("Cancelación registrada | Eventix"),
                contains("cancelada")
        );
    }
}
