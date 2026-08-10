package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServicePurchaseTest {

    @Test
    void shouldIncludeSaleCode() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendPurchaseConfirmation("customer@example.com", "SALE-901");

        verify(gateway).send(
                "customer@example.com",
                "Compra confirmada | Eventix",
                "Tu compra SALE-901 ha sido procesada correctamente. Tu boleta está disponible en Eventix."
        );
    }
}
