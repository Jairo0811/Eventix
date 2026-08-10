package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceGatewayTest {

    @Test
    void eachNotificationShouldDelegateExactlyOnce() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "RSV-1");

        verify(gateway, times(1)).send(
                "user@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-1 ha sido confirmada correctamente."
        );
    }
}
