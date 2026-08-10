package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmailGatewayBasicTest {

    @Test
    void gatewayContractShouldBeAvailable() {
        assertNotNull(EmailGateway.class);
    }
}
