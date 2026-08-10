package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoOpEmailGatewayTest {

    private final NoOpEmailGateway gateway = new NoOpEmailGateway();

    @Test
    void shouldIgnoreDeliveryWhenEmailIsDisabled() {
        assertDoesNotThrow(() -> gateway.send(
                "customer@example.com",
                "Eventix",
                "Notification"
        ));
    }
}
