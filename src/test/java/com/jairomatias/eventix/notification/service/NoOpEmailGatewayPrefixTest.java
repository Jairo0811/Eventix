package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoOpEmailGatewayPrefixTest {

    @Test
    void noOpConditionUsesNotificationPrefix() {
        ConditionalOnProperty condition = NoOpEmailGateway.class.getAnnotation(ConditionalOnProperty.class);
        assertEquals("eventix.notifications.email", condition.prefix());
    }
}
