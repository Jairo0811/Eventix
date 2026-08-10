package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationServiceSmokeTest {

    @Test
    void shouldConstructWithGateway() {
        NotificationService service = new NotificationService(new NoOpEmailGateway());

        assertNotNull(service);
    }
}
