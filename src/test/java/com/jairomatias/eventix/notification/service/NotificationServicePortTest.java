package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServicePortTest {

    @Test
    void smtpAndNoOpShouldShareTheSamePort() {
        assertTrue(EmailGateway.class.isAssignableFrom(SmtpEmailGateway.class));
        assertTrue(EmailGateway.class.isAssignableFrom(NoOpEmailGateway.class));
    }
}
