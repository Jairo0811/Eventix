package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpTest {

    @Test
    void allNotificationsShouldBeSafeWhenEmailIsDisabled() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> {
            service.sendReservationConfirmation("user@example.com", "RSV-1");
            service.sendPurchaseConfirmation("user@example.com", "SALE-1");
            service.sendCancellation("user@example.com", "RSV-1");
            service.sendRefundConfirmation("user@example.com", "SALE-1");
            service.sendEventReminder("user@example.com", "Eventix Live");
        });
    }
}
