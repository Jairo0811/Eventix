package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpRefundTest {

    @Test
    void refundNotificationShouldBeSafeWithNoOpGateway() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendRefundConfirmation("buyer@example.com", "R-2"));
    }
}
