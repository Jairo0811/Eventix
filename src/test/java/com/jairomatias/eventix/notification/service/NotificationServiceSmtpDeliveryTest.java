package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceSmtpDeliveryTest {

    @Test
    void serviceShouldDeliverThroughSmtpAdapter() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailGateway gateway = new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "no-reply@eventix.com")
        );
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "RSV-1");

        verify(sender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
