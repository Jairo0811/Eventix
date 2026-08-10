package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SmtpEmailGatewayConstructorTest {

    @Test
    void constructorShouldRequireSenderAndProperties() {
        Class<?>[] parameters = SmtpEmailGateway.class.getDeclaredConstructors()[0].getParameterTypes();

        assertArrayEquals(
                new Class<?>[]{JavaMailSender.class, NotificationProperties.class},
                parameters
        );
    }
}
