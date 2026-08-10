package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceSmtpRecipientTest {

    @Test
    void serviceShouldUseRequestedRecipientThroughAdapter() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(new SmtpEmailGateway(
                sender,
                new NotificationProperties(true, "no-reply@eventix.com")
        ));

        service.sendPurchaseConfirmation("buyer@example.com", "SALE-1");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertArrayEquals(new String[]{"buyer@example.com"}, captor.getValue().getTo());
    }
}
