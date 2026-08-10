package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceParameterContractTest {

    @Test
    void initialOperationsShouldAcceptRecipientAndReference() {
        assertTrue(Arrays.stream(NotificationService.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("send"))
                .allMatch(method -> method.getParameterCount() == 2));
    }
}
