package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceReferenceIsolationTest {

    @Test
    void reservationAndSaleReferencesShouldRemainIndependent() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "RSV-777");
        service.sendPurchaseConfirmation("user@example.com", "SALE-888");

        verify(gateway).send(
                "user@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-777 ha sido confirmada correctamente."
        );
        verify(gateway).send(
                "user@example.com",
                "Compra confirmada | Eventix",
                "Tu compra SALE-888 ha sido procesada correctamente. Tu boleta está disponible en Eventix."
        );
    }
}
