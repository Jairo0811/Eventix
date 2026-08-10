package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceConfigIsolationTest {

    @Test
    void notificationPropertiesShouldBeDedicatedConfigurationType() {
        assertTrue(NotificationProperties.class.isRecord());
    }
}
