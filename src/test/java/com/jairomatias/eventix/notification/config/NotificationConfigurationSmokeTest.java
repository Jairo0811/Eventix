package com.jairomatias.eventix.notification.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationConfigurationSmokeTest {

    @Test
    void shouldConstructConfiguration() {
        assertNotNull(new NotificationConfiguration());
    }
}
