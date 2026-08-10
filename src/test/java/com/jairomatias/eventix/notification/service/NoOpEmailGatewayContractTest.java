package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class NoOpEmailGatewayContractTest {

    @Test
    void shouldImplementEmailGateway() {
        assertInstanceOf(EmailGateway.class, new NoOpEmailGateway());
    }
}
