package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailGatewayMessageTest {

    @Test
    void shouldBuildMessageWithConfiguredSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailGateway gateway = new SmtpEmailGateway(
                mailSender,
                new NotificationProperties(true, "no-reply@eventix.com")
        );

        gateway.send("customer@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertEquals("no-reply@eventix.com", message.getFrom());
        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("Subject", message.getSubject());
        assertEquals("Body", message.getText());
    }
}
