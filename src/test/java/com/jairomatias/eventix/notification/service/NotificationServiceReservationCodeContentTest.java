package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReservationCodeContentTest {

    @Test
    void reservationShouldContainExactCode() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "RSV-2026-999");

        verify(gateway).send(
                eq("user@example.com"),
                eq("Reserva confirmada | Eventix"),
                contains("RSV-2026-999")
        );
    }
}
