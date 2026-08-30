package com.jairomatias.eventix.category.dto;

import com.jairomatias.eventix.category.entity.EventCategorySystemKey;

public record CategoryOption(
        Long id,
        String name,
        EventCategorySystemKey systemKey) {
}
