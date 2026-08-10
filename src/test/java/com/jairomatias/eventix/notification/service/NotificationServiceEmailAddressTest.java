package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServiceEmailAddressTest {

    @Test
    void shouldPreserveSenderAddressExactly() {
        NotificationProperties properties = new NotificationProperties(true, "no-reply@eventix.do");

        assertEquals("no-reply@eventix.do", properties.from());
    }
}
