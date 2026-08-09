package com.jairomatias.eventix.reporting.dto;

import java.math.BigDecimal;

public record OrganizerReportRow(
        Long organizerId,
        String organizerName,
        long events,
        long sales,
        long ticketsSold,
        long reservations,
        long attendees,
        BigDecimal revenue) {
}
