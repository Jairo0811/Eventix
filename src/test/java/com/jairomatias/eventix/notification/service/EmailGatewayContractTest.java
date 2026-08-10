package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailGatewayContractTest {

    @Test
    void implementationsShouldRespectEmailGatewayContract() {
        assertTrue(EmailGateway.class.isAssignableFrom(NoOpEmailGateway.class));
        assertTrue(EmailGateway.class.isAssignableFrom(SmtpEmailGateway.class));
    }
}
