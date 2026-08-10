package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceSmtpSubjectTest {

    @Test
    void serviceShouldBuildExpectedSubjectThroughAdapter() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "no-reply@eventix.com")
        ));

        service.sendReservationConfirmation("buyer@example.com", "RSV-1");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("Reserva confirmada | Eventix", captor.getValue().getSubject());
    }
}
