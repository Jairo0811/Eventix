package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReminderRecipientTest {

    @Test
    void reminderShouldGoToRequestedRecipient() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendEventReminder("guest@example.com", "Expo");

        verify(gateway).send(
                "guest@example.com",
                "Tu evento se acerca | Eventix",
                "Recuerda que Expo se aproxima. Consulta tu boleta en Eventix antes de llegar."
        );
    }
}
