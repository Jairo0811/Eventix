package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceFallbackTest {

    @Test
    void noOpGatewayShouldAllowLocalDevelopment() {
        EmailGateway gateway = new NoOpEmailGateway();
        NotificationService service = new NotificationService(gateway);

        assertDoesNotThrow(() -> service.sendEventReminder(
                "dev@example.com",
                "Local Event"
        ));
    }
}
