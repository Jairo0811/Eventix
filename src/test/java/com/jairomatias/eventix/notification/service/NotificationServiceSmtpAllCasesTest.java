package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceSmtpAllCasesTest {

    @Test
    void allInitialCasesShouldReachMailSender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "no-reply@eventix.com")
        ));

        service.sendReservationConfirmation("user@example.com", "R");
        service.sendPurchaseConfirmation("user@example.com", "S");
        service.sendCancellation("user@example.com", "C");
        service.sendRefundConfirmation("user@example.com", "F");
        service.sendEventReminder("user@example.com", "E");

        verify(sender, times(5)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
