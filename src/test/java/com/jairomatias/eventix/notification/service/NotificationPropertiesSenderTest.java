package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPropertiesSenderTest {

    @Test
    void shouldKeepConfiguredSender() {
        NotificationProperties properties = new NotificationProperties(
                true,
                "tickets@eventix.com"
        );

        assertEquals("tickets@eventix.com", properties.from());
    }
}
