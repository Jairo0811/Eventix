package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationPropertiesContractTest {

    @Test
    void shouldConstructProperties() {
        assertNotNull(new NotificationProperties(false, "no-reply@eventix.local"));
    }
}
