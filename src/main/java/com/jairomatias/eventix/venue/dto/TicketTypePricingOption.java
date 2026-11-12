package com.jairomatias.eventix.venue.dto;

import java.math.BigDecimal;

public record TicketTypePricingOption(
        Long id,
        String name,
        BigDecimal price) {
}
