package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationServiceDisabledFlagTest {

    @Test
    void shouldExposeDisabledFlag() {
        assertFalse(new NotificationProperties(false, "sender@eventix.com").enabled());
    }
}
