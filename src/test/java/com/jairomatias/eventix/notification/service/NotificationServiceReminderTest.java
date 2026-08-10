package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReminderTest {

    @Test
    void shouldIncludeEventName() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("customer@example.com", "Festival 2026");

        verify(gateway).send(
                "customer@example.com",
                "Tu evento se acerca | Eventix",
                "Recuerda que Festival 2026 se aproxima. Consulta tu boleta en Eventix antes de llegar."
        );
    }
}
