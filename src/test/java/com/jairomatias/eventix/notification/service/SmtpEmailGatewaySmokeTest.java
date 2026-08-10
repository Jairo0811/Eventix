package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SmtpEmailGatewaySmokeTest {

    @Test
    void shouldConstructSmtpGateway() {
        SmtpEmailGateway gateway = new SmtpEmailGateway(
                mock(JavaMailSender.class),
                new NotificationProperties(true, "no-reply@eventix.com")
        );

        assertNotNull(gateway);
    }
}
