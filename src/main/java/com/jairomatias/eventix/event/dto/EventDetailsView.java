package com.jairomatias.eventix.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.event.entity.EventStatus;

public record EventDetailsView(
        Long id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        EventStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String venue,
        String address,
        int capacity,
        Long organizerId,
        String organizerName,
        String coverImageUrl,
        boolean freeEvent,
        BigDecimal basePrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {
}
