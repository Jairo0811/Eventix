package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceAllCasesTest {

    @Test
    void shouldSupportAllInitialTransactionalNotifications() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendReservationConfirmation("user@example.com", "RSV-1");
        service.sendPurchaseConfirmation("user@example.com", "SALE-1");
        service.sendCancellation("user@example.com", "RSV-1");
        service.sendRefundConfirmation("user@example.com", "SALE-1");
        service.sendEventReminder("user@example.com", "Festival");

        verify(gateway, times(5)).send(
                org.mockito.ArgumentMatchers.eq("user@example.com"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
