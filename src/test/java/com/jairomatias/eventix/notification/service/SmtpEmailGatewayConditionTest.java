package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmtpEmailGatewayConditionTest {

    @Test
    void smtpShouldRequireEmailEnabled() {
        ConditionalOnProperty annotation = SmtpEmailGateway.class
                .getAnnotation(ConditionalOnProperty.class);

        assertEquals("true", annotation.havingValue());
    }
}
