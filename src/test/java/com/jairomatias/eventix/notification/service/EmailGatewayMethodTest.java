package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EmailGatewayMethodTest {

    @Test
    void sendMethodShouldRemainAvailable() {
        assertDoesNotThrow(() -> EmailGateway.class.getMethod(
                "send", String.class, String.class, String.class));
    }
}
