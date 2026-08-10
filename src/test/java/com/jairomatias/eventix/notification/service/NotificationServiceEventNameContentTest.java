package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceEventNameContentTest {

    @Test
    void reminderShouldContainExactEventName() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("user@example.com", "Tech Summit RD 2026");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Tu evento se acerca | Eventix"),
                contains("Tech Summit RD 2026")
        );
    }
}
