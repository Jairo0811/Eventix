package com.jairomatias.eventix.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eventix.notifications.email")
public record NotificationProperties(
        boolean enabled,
        String from
) {
}
