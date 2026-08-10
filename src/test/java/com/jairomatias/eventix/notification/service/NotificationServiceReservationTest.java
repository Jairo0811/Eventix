package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReservationTest {

    @Test
    void shouldIncludeReservationCode() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("customer@example.com", "RSV-901");

        verify(gateway).send(
                "customer@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-901 ha sido confirmada correctamente."
        );
    }
}
