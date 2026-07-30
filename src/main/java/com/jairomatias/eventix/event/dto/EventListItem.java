package com.jairomatias.eventix.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.event.entity.EventStatus;

public record EventListItem(
        Long id,
        String title,
        String categoryName,
        EventStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String venue,
        int capacity,
        String organizerName,
        String coverImageUrl,
        boolean freeEvent,
        BigDecimal basePrice) {
}
