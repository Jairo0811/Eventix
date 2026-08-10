package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceCancellationSubjectTest {

    @Test
    void shouldUseCancellationSubject() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendCancellation("user@example.com", "RSV-1");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Cancelación registrada | Eventix"),
                anyString()
        );
    }
}
