package com.jairomatias.eventix.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.reporting.dto.CategoryReportRow;
import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.MonthlyRevenueRow;
import com.jairomatias.eventix.reporting.dto.OrganizerReportRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportSummary;

class ReportPdfServiceTest {

    private final ReportPdfService service = new ReportPdfService();

    @Test
    void createsReadableBrandedExecutivePdf() throws Exception {
        byte[] content = service.create(dataset());
        assertThat(new String(content, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(content.length).isGreaterThan(2_000);
        try (PDDocument document = Loader.loadPDF(content)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void createsPdfWhenDatasetHasNoEvents() throws Exception {
        ReportDataset source = dataset();
        ReportDataset empty = new ReportDataset(source.from(), source.to(), source.summary(),
                List.of(), source.byCategory(), source.byOrganizer(), source.monthlyRevenue());
        byte[] content = service.create(empty);
        try (PDDocument document = Loader.loadPDF(content)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void preservesLongReportTextWithoutEllipsisAndPaginates() throws Exception {
        ReportDataset base = dataset();
        List<EventReportRow> events = IntStream.rangeClosed(1, 12)
                .mapToObj(index -> new EventReportRow(
                        (long) index,
                        "Congreso Internacional de Innovación Tecnológica, Entretenimiento y Experiencias Digitales Eventix " + index,
                        2L,
                        "Conferencia profesional de tecnología y transformación digital",
                        3L,
                        "Organización Dominicana de Producción de Eventos, Tecnología y Entretenimiento Profesional",
                        2000000,
                        4000000,
                        3000000,
                        5000000,
                        4000000,
                        new BigDecimal("9876543210.50")))
                .toList();
        ReportDataset longDataset = new ReportDataset(base.from(), base.to(), base.summary(),
                events, base.byCategory(), base.byOrganizer(), base.monthlyRevenue());

        byte[] content = service.create(longDataset);

        try (PDDocument document = Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text).doesNotContain("...");
            assertThat(text).contains("Congreso Internacional", "Eventix - Sistema de Reportes");
        }
    }

    private ReportDataset dataset() {
        BigDecimal revenue = new BigDecimal("1250.50");
        EventReportRow event = new EventReportRow(1L, "Evento profesional", 2L,
                "Conferencia", 3L, "Organizador Eventix", 2, 4, 3, 5, 4, revenue);
        return new ReportDataset(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new ReportSummary(revenue, 2, 4, 3, 5, 4,
                        new BigDecimal("66.67"), new BigDecimal("100.00")),
                List.of(event),
                List.of(new CategoryReportRow(2L, "Conferencia", 1, 2, 4, 3, 4, revenue)),
                List.of(new OrganizerReportRow(3L, "Organizador Eventix", 1, 2, 4, 3, 4, revenue)),
                List.of(new MonthlyRevenueRow(2026, 8, "Agosto 2026", 2, 4, revenue)));
    }
}
