package com.jairomatias.eventix.reporting.dto;

import java.math.BigDecimal;

public record EventReportRow(
        Long eventId,
        String eventName,
        Long categoryId,
        String categoryName,
        Long organizerId,
        String organizerName,
        long sales,
        long ticketsSold,
        long reservations,
        long reservedPlaces,
        long attendees,
        BigDecimal revenue) {
}
