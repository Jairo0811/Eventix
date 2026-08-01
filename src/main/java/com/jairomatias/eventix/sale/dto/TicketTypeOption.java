package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;

public record TicketTypeOption(
        Long id,
        String name,
        BigDecimal price,
        int availableQuantity) {
}
