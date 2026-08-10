package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmtpEmailGatewayPrefixTest {

    @Test
    void smtpConditionUsesNotificationPrefix() {
        ConditionalOnProperty condition = SmtpEmailGateway.class.getAnnotation(ConditionalOnProperty.class);
        assertEquals("eventix.notifications.email", condition.prefix());
    }
}
