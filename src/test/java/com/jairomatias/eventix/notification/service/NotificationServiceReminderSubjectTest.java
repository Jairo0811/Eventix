package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReminderSubjectTest {

    @Test
    void shouldUseReminderSubject() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("user@example.com", "Expo");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Tu evento se acerca | Eventix"),
                anyString()
        );
    }
}
