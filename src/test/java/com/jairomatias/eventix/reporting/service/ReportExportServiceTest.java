package com.jairomatias.eventix.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.reporting.dto.CategoryReportRow;
import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.MonthlyRevenueRow;
import com.jairomatias.eventix.reporting.dto.OrganizerReportRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportSummary;

class ReportExportServiceTest {

    private final ReportExportService service = new ReportExportService();

    @Test
    void exportsCsvWithUtf8BomAndEventData() {
        byte[] content = service.toCsv(dataset());
        String csv = new String(content, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFFReporte Eventix");
        assertThat(csv).contains("Evento profesional", "1250.50");
    }

    @Test
    void exportsValidXlsxPackage() throws Exception {
        byte[] content = service.toXlsx(dataset());

        assertThat(content).startsWith((byte) 'P', (byte) 'K');
        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(content))) {
            assertThat(zip.getNextEntry()).isNotNull();
        }
    }

    @Test
    void exportsPdfDocument() {
        byte[] content = service.toPdf(dataset());

        assertThat(new String(content, 0, 4, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");
        assertThat(content.length).isGreaterThan(500);
    }

    private ReportDataset dataset() {
        BigDecimal revenue = new BigDecimal("1250.50");
        EventReportRow event = new EventReportRow(
                1L,
                "Evento profesional",
                2L,
                "Conferencia",
                3L,
                "Organizador Eventix",
                2,
                4,
                3,
                5,
                4,
                revenue);
        return new ReportDataset(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new ReportSummary(
                        revenue,
                        2,
                        4,
                        3,
                        5,
                        4,
                        new BigDecimal("66.67"),
                        new BigDecimal("100.00")),
                List.of(event),
                List.of(new CategoryReportRow(
                        2L, "Conferencia", 1, 2, 4, 3, 4, revenue)),
                List.of(new OrganizerReportRow(
                        3L, "Organizador Eventix", 1, 2, 4, 3, 4, revenue)),
                List.of(new MonthlyRevenueRow(
                        2026, 8, "Agosto 2026", 2, 4, revenue)));
    }
}
