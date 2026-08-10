package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceSmtpRefundTest {

    @Test
    void refundShouldDeliverThroughSmtpAdapter() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "no-reply@eventix.com")
        ));

        service.sendRefundConfirmation("user@example.com", "REF-1");

        verify(sender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
