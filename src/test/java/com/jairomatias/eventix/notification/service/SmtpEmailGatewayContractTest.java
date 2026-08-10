package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class SmtpEmailGatewayContractTest {

    @Test
    void shouldImplementEmailGateway() {
        SmtpEmailGateway gateway = new SmtpEmailGateway(
                mock(JavaMailSender.class),
                new NotificationProperties(true, "no-reply@eventix.com")
        );

        assertInstanceOf(EmailGateway.class, gateway);
    }
}
