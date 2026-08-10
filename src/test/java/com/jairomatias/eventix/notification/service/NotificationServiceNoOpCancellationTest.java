package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpCancellationTest {

    @Test
    void cancellationShouldBeSafeWithNoOpGateway() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendCancellation("buyer@example.com", "C-2"));
    }
}
