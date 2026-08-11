package com.jairomatias.eventix.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "eventix.notifications.email")
@Validated
public record NotificationProperties(
        boolean enabled,
        @NotBlank @Email String from
) {
}
