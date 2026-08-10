package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceConstructionTest {

    @Test
    void shouldAcceptGatewayDependency() {
        assertDoesNotThrow(() -> new NotificationService(new NoOpEmailGateway()));
    }
}
