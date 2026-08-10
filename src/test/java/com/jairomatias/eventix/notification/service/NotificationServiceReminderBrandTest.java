package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReminderBrandTest {

    @Test
    void reminderShouldIdentifyEventix() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("user@example.com", "Festival");

        verify(gateway).send(
                eq("user@example.com"),
                contains("Eventix"),
                contains("Festival")
        );
    }
}
