package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServicePurchaseCountTest {

    @Test
    void purchaseShouldProduceExactlyOneEmail() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendPurchaseConfirmation("user@example.com", "SALE-2");

        verify(gateway, times(1)).send(anyString(), anyString(), anyString());
    }
}
