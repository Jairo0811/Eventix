package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceMultipleRecipientsTest {

    @Test
    void shouldNotMixRecipients() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("one@example.com", "RSV-1");
        service.sendReservationConfirmation("two@example.com", "RSV-2");

        verify(gateway).send(
                "one@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-1 ha sido confirmada correctamente."
        );
        verify(gateway).send(
                "two@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-2 ha sido confirmada correctamente."
        );
    }
}
