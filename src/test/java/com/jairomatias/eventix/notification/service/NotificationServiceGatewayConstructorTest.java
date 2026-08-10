package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NotificationServiceGatewayConstructorTest {

    @Test
    void constructorShouldRequireOnlyEmailGateway() {
        Class<?>[] parameters = NotificationService.class.getDeclaredConstructors()[0].getParameterTypes();

        assertArrayEquals(new Class<?>[]{EmailGateway.class}, parameters);
    }
}
