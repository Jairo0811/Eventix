package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServiceApiTest {

    @Test
    void shouldExposeSixTransactionalUseCases() {
        long methods = java.util.Arrays.stream(NotificationService.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("send"))
                .count();

        assertEquals(6, methods);
    }
}
