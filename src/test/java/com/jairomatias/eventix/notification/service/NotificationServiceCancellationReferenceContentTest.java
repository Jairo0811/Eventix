package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceCancellationReferenceContentTest {

    @Test
    void cancellationShouldContainExactReference() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendCancellation("user@example.com", "CANCEL-2026-999");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Cancelación registrada | Eventix"),
                contains("CANCEL-2026-999")
        );
    }
}
