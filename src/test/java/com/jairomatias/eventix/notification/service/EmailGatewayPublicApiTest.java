package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailGatewayPublicApiTest {

    @Test
    void sendOperationShouldBePublic() throws NoSuchMethodException {
        var method = EmailGateway.class.getMethod("send", String.class, String.class, String.class);
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }
}
