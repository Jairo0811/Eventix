package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailGatewayTest {

    @Test
    void shouldDelegateMessageToMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationProperties properties = new NotificationProperties(
                true,
                "no-reply@eventix.com"
        );
        SmtpEmailGateway gateway = new SmtpEmailGateway(mailSender, properties);

        gateway.send("customer@example.com", "Your ticket", "Ticket ready");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
