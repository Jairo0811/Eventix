package com.jairomatias.eventix.reporting.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportDataset(
        LocalDate from,
        LocalDate to,
        ReportSummary summary,
        List<EventReportRow> byEvent,
        List<CategoryReportRow> byCategory,
        List<OrganizerReportRow> byOrganizer,
        List<MonthlyRevenueRow> monthlyRevenue) {
}
