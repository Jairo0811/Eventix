package com.jairomatias.eventix.venue.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SeatHoldResult(
        String holdToken,
        LocalDateTime expiresAt,
        List<Long> seatIds) {
}
