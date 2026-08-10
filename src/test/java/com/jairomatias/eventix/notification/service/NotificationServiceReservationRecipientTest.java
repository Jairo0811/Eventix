package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReservationRecipientTest {

    @Test
    void reservationShouldGoToRequestedRecipient() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("buyer@example.com", "RSV-40");

        verify(gateway).send(
                "buyer@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-40 ha sido confirmada correctamente."
        );
    }
}
