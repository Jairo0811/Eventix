package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationPropertiesDisabledTest {

    @Test
    void shouldSupportDisabledEmailDelivery() {
        NotificationProperties properties = new NotificationProperties(
                false,
                "no-reply@eventix.local"
        );

        assertFalse(properties.enabled());
    }
}
