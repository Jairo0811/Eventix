package com.jairomatias.eventix.reservation.dto;

import java.time.LocalDateTime;

public record EventReservationOption(
        Long id,
        String title,
        LocalDateTime startAt,
        int capacity) {
}
