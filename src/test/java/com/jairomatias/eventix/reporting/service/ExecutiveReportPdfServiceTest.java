package com.jairomatias.eventix.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.reporting.dto.CategoryReportRow;
import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.MonthlyRevenueRow;
import com.jairomatias.eventix.reporting.dto.OrganizerReportRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportSummary;

class ExecutiveReportPdfServiceTest {

    private final ExecutiveReportPdfService service =
            new ExecutiveReportPdfService();

    @Test
    void exportsProfessionalPdfDocument() {
        byte[] content = service.toPdf(dataset());

        assertThat(new String(content, 0, 4, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");
        assertThat(content.length).isGreaterThan(2_000);
    }

    private ReportDataset dataset() {
        BigDecimal revenue = new BigDecimal("2500.00");
        EventReportRow event = new EventReportRow(
                1L,
                "WWE",
                2L,
                "Deportivo",
                3L,
                "Organizador Eventix",
                1,
                1,
                1,
                1,
                0,
                revenue);
        return new ReportDataset(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 8, 8),
                new ReportSummary(
                        revenue,
                        1,
                        1,
                        1,
                        1,
                        0,
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO),
                List.of(event),
                List.of(new CategoryReportRow(
                        2L, "Deportivo", 1, 1, 1, 1, 0, revenue)),
                List.of(new OrganizerReportRow(
                        3L, "Organizador Eventix",
                        1, 1, 1, 1, 0, revenue)),
                List.of(new MonthlyRevenueRow(
                        2026, 8, "Agosto 2026", 1, 1, revenue)));
    }
}
