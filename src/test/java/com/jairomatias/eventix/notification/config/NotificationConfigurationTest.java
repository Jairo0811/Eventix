package com.jairomatias.eventix.notification.config;

import com.jairomatias.eventix.notification.service.NotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationConfigurationTest {

    @Test
    void shouldEnableNotificationProperties() {
        EnableConfigurationProperties annotation = NotificationConfiguration.class
                .getAnnotation(EnableConfigurationProperties.class);

        assertNotNull(annotation);
        assertNotNull(NotificationProperties.class);
    }
}
