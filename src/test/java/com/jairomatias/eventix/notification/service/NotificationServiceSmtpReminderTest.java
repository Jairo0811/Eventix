package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceSmtpReminderTest {

    @Test
    void reminderShouldDeliverThroughSmtpAdapter() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "no-reply@eventix.com")
        ));

        service.sendEventReminder("user@example.com", "Expo");

        verify(sender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
