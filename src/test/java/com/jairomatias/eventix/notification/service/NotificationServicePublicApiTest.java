package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServicePublicApiTest {

    @Test
    void transactionalOperationsShouldBePublic() {
        assertTrue(Arrays.stream(NotificationService.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("send"))
                .allMatch(method -> Modifier.isPublic(method.getModifiers())));
    }
}
