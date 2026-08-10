package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceStringParameterTest {

    @Test
    void initialOperationsShouldUseStringValues() {
        assertTrue(Arrays.stream(NotificationService.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("send"))
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .allMatch(String.class::equals));
    }
}
