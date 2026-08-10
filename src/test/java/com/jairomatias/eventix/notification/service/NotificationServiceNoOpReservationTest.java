package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpReservationTest {

    @Test
    void reservationShouldBeSafeWithNoOpGateway() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendReservationConfirmation("buyer@example.com", "R-2"));
    }
}
