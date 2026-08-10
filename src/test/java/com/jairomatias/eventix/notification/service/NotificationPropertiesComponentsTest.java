package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPropertiesComponentsTest {

    @Test
    void recordComponentsShouldRemainStable() {
        assertEquals("enabled", NotificationProperties.class.getRecordComponents()[0].getName());
        assertEquals("from", NotificationProperties.class.getRecordComponents()[1].getName());
    }
}
