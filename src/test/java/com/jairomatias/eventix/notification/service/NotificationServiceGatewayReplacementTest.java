package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class NotificationServiceGatewayReplacementTest {

    @Test
    void serviceShouldAcceptNoOpAndSmtpAdapters() {
        assertDoesNotThrow(() -> new NotificationService(new NoOpEmailGateway()));
        assertDoesNotThrow(() -> new NotificationService(new SmtpEmailGateway(
                mock(JavaMailSender.class),
                new NotificationProperties(true, "no-reply@eventix.com")
        )));
    }
}
