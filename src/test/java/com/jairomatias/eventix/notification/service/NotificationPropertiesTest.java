package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPropertiesTest {

    @Test
    void shouldExposeConfiguredValues() {
        NotificationProperties properties = new NotificationProperties(
                true,
                "no-reply@eventix.com"
        );

        assertTrue(properties.enabled());
        assertEquals("no-reply@eventix.com", properties.from());
    }
}
