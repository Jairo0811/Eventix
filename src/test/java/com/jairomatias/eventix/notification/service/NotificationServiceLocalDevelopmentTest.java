package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceLocalDevelopmentTest {

    @Test
    void localDevelopmentShouldWorkWithEmailDisabled() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendReservationConfirmation("local@example.com", "LOCAL-1"));
    }
}
