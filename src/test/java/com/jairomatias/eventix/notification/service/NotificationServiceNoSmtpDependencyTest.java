package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceNoSmtpDependencyTest {

    @Test
    void businessServiceShouldNotRequireMailSender() {
        assertDoesNotThrow(() -> new NotificationService(new NoOpEmailGateway()));
    }
}
