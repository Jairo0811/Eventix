package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPropertiesApiTest {

    @Test
    void propertiesShouldExposeEnabledAndFrom() {
        assertEquals(2, NotificationProperties.class.getRecordComponents().length);
    }
}
