package com.jairomatias.eventix.checkout.dto;

import java.math.BigDecimal;

public record CustomerTicketOption(
        Long id,
        String name,
        String category,
        BigDecimal price,
        int availableQuantity) {
}
