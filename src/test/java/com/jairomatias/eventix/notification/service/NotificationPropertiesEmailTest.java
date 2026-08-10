package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPropertiesEmailTest {

    @Test
    void shouldExposeEmailAddress() {
        NotificationProperties properties = new NotificationProperties(
                true,
                "mailer@eventix.com"
        );

        assertEquals("mailer@eventix.com", properties.from());
    }
}
