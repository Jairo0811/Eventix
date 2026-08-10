package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceRecipientTest {

    @Test
    void shouldPreserveRecipientForReminder() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("attendee@example.com", "Eventix Summit");

        verify(gateway).send(
                eq("attendee@example.com"),
                eq("Tu evento se acerca | Eventix"),
                eq("Recuerda que Eventix Summit se aproxima. Consulta tu boleta en Eventix antes de llegar.")
        );
    }
}
