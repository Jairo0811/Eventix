package com.jairomatias.eventix.ticket.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

public record AppleLogRequest(
        @Size(max = 50)
        List<@Size(max = 500) String> logs) {
}
