package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationServiceTypeTest {

    @Test
    void shouldBeSpringService() {
        assertNotNull(NotificationService.class.getAnnotation(Service.class));
    }
}
