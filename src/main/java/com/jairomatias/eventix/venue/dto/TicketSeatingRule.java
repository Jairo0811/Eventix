package com.jairomatias.eventix.venue.dto;

import java.math.BigDecimal;

public record TicketSeatingRule(
        boolean requiresSeatHold,
        Long sectionId,
        BigDecimal unitPrice) {
}
