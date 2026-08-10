package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceArchitectureTest {

    @Test
    void notificationServiceShouldDependOnPortAbstraction() {
        assertTrue(EmailGateway.class.isInterface());
        assertTrue(EmailGateway.class.isAssignableFrom(NoOpEmailGateway.class));
        assertTrue(EmailGateway.class.isAssignableFrom(SmtpEmailGateway.class));
    }
}
