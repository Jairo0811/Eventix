package com.jairomatias.eventix.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationConfigurationTypeTest {

    @Test
    void shouldBeSpringConfiguration() {
        assertNotNull(NotificationConfiguration.class.getAnnotation(Configuration.class));
    }
}
