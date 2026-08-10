package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceExactCountTest {

    @Test
    void purchaseShouldProduceOneEmail() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService service = new NotificationService(gateway);

        service.sendPurchaseConfirmation("user@example.com", "SALE-1");

        verify(gateway, times(1)).send(anyString(), anyString(), anyString());
    }
}
