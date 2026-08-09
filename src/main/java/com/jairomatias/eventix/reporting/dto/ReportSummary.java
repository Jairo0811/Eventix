package com.jairomatias.eventix.reporting.dto;

import java.math.BigDecimal;

public record ReportSummary(
        BigDecimal revenue,
        long sales,
        long ticketsSold,
        long reservations,
        long reservedPlaces,
        long attendees,
        BigDecimal conversionRate,
        BigDecimal attendanceRate) {
}
