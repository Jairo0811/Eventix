package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceFiveCasesTest {

    @Test
    void shouldExposeInitialNotificationUseCases() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> {
            service.sendReservationConfirmation("a@example.com", "R1");
            service.sendPurchaseConfirmation("a@example.com", "S1");
            service.sendCancellation("a@example.com", "R1");
            service.sendRefundConfirmation("a@example.com", "S1");
            service.sendEventReminder("a@example.com", "Event");
        });
    }
}
