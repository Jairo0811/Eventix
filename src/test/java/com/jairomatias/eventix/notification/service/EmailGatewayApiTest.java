package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailGatewayApiTest {

    @Test
    void gatewayShouldExposeSingleDeliveryOperation() {
        assertEquals(1, EmailGateway.class.getDeclaredMethods().length);
    }
}
