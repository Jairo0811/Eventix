package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationServiceAdapterIsolationTest {

    @Test
    void noOpAndSmtpAdaptersShouldBeIndependentImplementations() {
        assertFalse(NoOpEmailGateway.class.isAssignableFrom(SmtpEmailGateway.class));
        assertFalse(SmtpEmailGateway.class.isAssignableFrom(NoOpEmailGateway.class));
    }
}
