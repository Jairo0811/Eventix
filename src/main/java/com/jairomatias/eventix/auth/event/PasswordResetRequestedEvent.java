package com.jairomatias.eventix.auth.event;

public record PasswordResetRequestedEvent(
        String email,
        String resetUrl) {
}
