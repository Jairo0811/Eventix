package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPropertiesPrefixTest {

    @Test
    void shouldUseEventixEmailPrefix() {
        ConfigurationProperties annotation = NotificationProperties.class
                .getAnnotation(ConfigurationProperties.class);

        assertEquals("eventix.notifications.email", annotation.prefix());
    }
}
