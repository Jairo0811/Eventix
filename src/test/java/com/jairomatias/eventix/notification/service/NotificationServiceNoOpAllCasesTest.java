package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpAllCasesTest {

    @Test
    void allCasesShouldRemainNonBlockingWithoutEmailProvider() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> {
            service.sendReservationConfirmation("user@example.com", "R");
            service.sendPurchaseConfirmation("user@example.com", "S");
            service.sendCancellation("user@example.com", "C");
            service.sendRefundConfirmation("user@example.com", "F");
            service.sendEventReminder("user@example.com", "E");
        });
    }
}
