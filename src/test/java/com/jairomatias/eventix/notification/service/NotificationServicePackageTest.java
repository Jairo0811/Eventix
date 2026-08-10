package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServicePackageTest {

    @Test
    void serviceShouldLiveInNotificationModule() {
        assertEquals(
                "com.jairomatias.eventix.notification.service",
                NotificationService.class.getPackageName()
        );
    }
}
