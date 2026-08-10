package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReminderContentTest {

    @Test
    void reminderMessageShouldMentionTicket() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("buyer@example.com", "Festival");

        verify(gateway).send(
                eq("buyer@example.com"),
                eq("Tu evento se acerca | Eventix"),
                contains("boleta")
        );
    }
}
