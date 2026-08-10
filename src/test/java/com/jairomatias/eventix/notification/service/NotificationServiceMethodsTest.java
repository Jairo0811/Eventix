package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceMethodsTest {

    @Test
    void initialMethodsShouldRemainAvailable() {
        assertDoesNotThrow(() -> NotificationService.class.getMethod(
                "sendReservationConfirmation", String.class, String.class));
        assertDoesNotThrow(() -> NotificationService.class.getMethod(
                "sendPurchaseConfirmation", String.class, String.class));
        assertDoesNotThrow(() -> NotificationService.class.getMethod(
                "sendCancellation", String.class, String.class));
        assertDoesNotThrow(() -> NotificationService.class.getMethod(
                "sendRefundConfirmation", String.class, String.class));
        assertDoesNotThrow(() -> NotificationService.class.getMethod(
                "sendEventReminder", String.class, String.class));
    }
}
