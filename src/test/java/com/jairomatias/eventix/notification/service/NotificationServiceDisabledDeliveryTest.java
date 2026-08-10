package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceDisabledDeliveryTest {

    @Test
    void disabledGatewayShouldNotBlockBusinessFlow() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendReservationConfirmation(
                "buyer@example.com",
                "RSV-500"
        ));
    }
}
