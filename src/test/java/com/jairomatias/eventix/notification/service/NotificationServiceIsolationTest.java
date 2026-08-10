package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceIsolationTest {

    @Test
    void shouldWorkWithoutSmtpWhenDeliveryIsDisabled() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendPurchaseConfirmation(
                "customer@example.com",
                "SALE-200"
        ));
    }
}
