package com.jairomatias.eventix.category.dto;

import java.time.LocalDateTime;

public record CategoryListItem(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt) {
}
