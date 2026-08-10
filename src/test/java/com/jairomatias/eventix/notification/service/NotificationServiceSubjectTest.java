package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceSubjectTest {

    @Test
    void subjectsShouldIdentifyEventix() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "RSV-1");

        verify(gateway).send(
                eq("user@example.com"),
                contains("Eventix"),
                contains("RSV-1")
        );
    }
}
