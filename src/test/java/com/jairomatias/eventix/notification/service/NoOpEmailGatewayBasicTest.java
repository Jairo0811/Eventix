package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoOpEmailGatewayBasicTest {

    @Test
    void noOpGatewayTypeShouldBeAvailable() {
        assertNotNull(NoOpEmailGateway.class);
    }
}
