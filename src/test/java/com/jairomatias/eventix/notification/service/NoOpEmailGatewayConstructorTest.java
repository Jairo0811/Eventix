package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoOpEmailGatewayConstructorTest {

    @Test
    void shouldHaveDefaultConstructorOnly() {
        assertEquals(0, NoOpEmailGateway.class.getDeclaredConstructors()[0].getParameterCount());
    }
}
