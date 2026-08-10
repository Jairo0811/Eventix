package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOpEmailGatewayConditionTest {

    @Test
    void noOpShouldMatchWhenPropertyIsMissing() {
        ConditionalOnProperty annotation = NoOpEmailGateway.class
                .getAnnotation(ConditionalOnProperty.class);

        assertTrue(annotation.matchIfMissing());
    }
}
