package com.jairomatias.eventix.notification.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationConfigurationBasicTest {

    @Test
    void configurationTypeShouldBeAvailable() {
        assertNotNull(NotificationConfiguration.class);
    }
}
