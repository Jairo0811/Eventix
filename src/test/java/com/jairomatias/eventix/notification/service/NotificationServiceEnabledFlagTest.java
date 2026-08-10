package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceEnabledFlagTest {

    @Test
    void shouldExposeEnabledFlag() {
        assertTrue(new NotificationProperties(true, "sender@eventix.com").enabled());
    }
}
