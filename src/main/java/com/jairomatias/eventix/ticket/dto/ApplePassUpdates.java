package com.jairomatias.eventix.ticket.dto;

import java.util.List;

public record ApplePassUpdates(
        List<String> serialNumbers,
        String lastUpdated) {
}
