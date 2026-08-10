package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationServiceMailSenderIsolationTest {

    @Test
    void notificationServiceConstructorShouldNotRequireJavaMailSender() {
        boolean requiresMailSender = Arrays.stream(NotificationService.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(type -> type.getName().equals("org.springframework.mail.javamail.JavaMailSender"));

        assertFalse(requiresMailSender);
    }
}
