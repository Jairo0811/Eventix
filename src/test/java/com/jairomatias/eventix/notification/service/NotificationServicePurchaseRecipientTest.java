package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServicePurchaseRecipientTest {

    @Test
    void purchaseShouldGoToRequestedRecipient() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendPurchaseConfirmation("buyer@example.com", "SALE-40");

        verify(gateway).send(
                "buyer@example.com",
                "Compra confirmada | Eventix",
                "Tu compra SALE-40 ha sido procesada correctamente. Tu boleta está disponible en Eventix."
        );
    }
}
