package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SmtpEmailGatewayTypeTest {

    @Test
    void shouldBeSpringComponent() {
        assertNotNull(SmtpEmailGateway.class.getAnnotation(Component.class));
    }
}
