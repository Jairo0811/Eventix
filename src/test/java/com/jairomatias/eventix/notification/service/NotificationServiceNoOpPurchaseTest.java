package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpPurchaseTest {

    @Test
    void purchaseNotificationShouldBeSafeWithNoOpGateway() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendPurchaseConfirmation("buyer@example.com", "S-2"));
    }
}
