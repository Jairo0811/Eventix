package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailGatewaySenderTest {

    @Test
    void shouldUseConfiguredFromAddress() {
        JavaMailSender sender = mock(JavaMailSender.class);
        SmtpEmailGateway gateway = new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "notifications@eventix.com")
        );

        gateway.send("user@example.com", "Eventix", "Hello");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("notifications@eventix.com", captor.getValue().getFrom());
    }
}
