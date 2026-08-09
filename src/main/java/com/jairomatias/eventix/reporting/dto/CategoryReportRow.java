package com.jairomatias.eventix.reporting.dto;

import java.math.BigDecimal;

public record CategoryReportRow(
        Long categoryId,
        String categoryName,
        long events,
        long sales,
        long ticketsSold,
        long reservations,
        long attendees,
        BigDecimal revenue) {
}
