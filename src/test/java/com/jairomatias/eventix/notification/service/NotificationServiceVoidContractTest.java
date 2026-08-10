package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServiceVoidContractTest {

    @Test
    void gatewaySendShouldReturnVoid() throws NoSuchMethodException {
        var method = EmailGateway.class.getMethod("send", String.class, String.class, String.class);
        assertEquals(void.class, method.getReturnType());
    }
}
