package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.sale.entity.TicketTypeCategory;

public record TicketTypeView(
        Long id,
        Long eventId,
        String eventTitle,
        TicketTypeCategory category,
        String name,
        BigDecimal price,
        int capacity,
        int allocatedQuantity,
        int availableQuantity,
        boolean active) {
}
