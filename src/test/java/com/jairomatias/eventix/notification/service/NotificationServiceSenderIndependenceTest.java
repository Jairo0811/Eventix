package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceSenderIndependenceTest {

    @Test
    void serviceShouldNotNeedSenderConfigurationDirectly() {
        assertDoesNotThrow(() -> new NotificationService(new NoOpEmailGateway()));
    }
}
