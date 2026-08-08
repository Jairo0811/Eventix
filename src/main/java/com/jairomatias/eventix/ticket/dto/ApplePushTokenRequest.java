package com.jairomatias.eventix.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplePushTokenRequest(
        @NotBlank
        @Size(max = 200)
        String pushToken) {
}
