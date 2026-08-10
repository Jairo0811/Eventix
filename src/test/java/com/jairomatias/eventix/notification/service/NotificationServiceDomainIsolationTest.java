package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationServiceDomainIsolationTest {

    @Test
    void notificationServiceShouldNotExtendSmtpAdapter() {
        assertFalse(SmtpEmailGateway.class.isAssignableFrom(NotificationService.class));
    }
}
