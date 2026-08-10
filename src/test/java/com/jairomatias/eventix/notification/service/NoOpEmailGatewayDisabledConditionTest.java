package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoOpEmailGatewayDisabledConditionTest {

    @Test
    void noOpShouldActivateWhenEmailIsFalse() {
        ConditionalOnProperty annotation = NoOpEmailGateway.class
                .getAnnotation(ConditionalOnProperty.class);

        assertEquals("false", annotation.havingValue());
    }
}
