package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoOpReminderTest {

    @Test
    void reminderShouldBeSafeWithNoOpGateway() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertDoesNotThrow(() -> service.sendEventReminder("buyer@example.com", "Expo"));
    }
}
