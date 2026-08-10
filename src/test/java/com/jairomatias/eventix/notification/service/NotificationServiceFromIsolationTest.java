package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServiceFromIsolationTest {

    @Test
    void senderShouldBelongToGatewayConfiguration() {
        NotificationProperties properties = new NotificationProperties(true, "sender@eventix.com");

        assertEquals("sender@eventix.com", properties.from());
    }
}
