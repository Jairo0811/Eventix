package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationServiceClassTest {

    @Test
    void serviceShouldBeConcreteClass() {
        assertFalse(NotificationService.class.isInterface());
    }
}
