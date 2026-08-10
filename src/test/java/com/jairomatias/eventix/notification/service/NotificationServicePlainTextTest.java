package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServicePlainTextTest {

    @Test
    void reservationShouldProduceTextMessage() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "R-1");

        verify(gateway).send(anyString(), anyString(), anyString());
    }
}
