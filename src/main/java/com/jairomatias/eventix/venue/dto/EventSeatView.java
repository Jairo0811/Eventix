package com.jairomatias.eventix.venue.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.venue.entity.EventSeatStatus;

public record EventSeatView(
        Long inventoryId,
        Long seatId,
        String sectionCode,
        String sectionName,
        String rowCode,
        String seatNumber,
        String label,
        boolean accessible,
        EventSeatStatus status,
        LocalDateTime holdExpiresAt) {
}
