package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoOpEmailGatewayTypeTest {

    @Test
    void shouldBeSpringComponent() {
        assertNotNull(NoOpEmailGateway.class.getAnnotation(Component.class));
    }
}
