package com.jairomatias.eventix.reporting.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.MonthlyRevenueRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportSummary;

@Service
public class ExecutiveReportPdfService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale REPORT_LOCALE = Locale.of("es", "DO");

    private static final Color NAVY = new Color(3, 31, 43);
    private static final Color DARK_GREEN = new Color(0, 92, 68);
    private static final Color EVENTIX_GREEN = new Color(31, 190, 111);
    private static final Color EVENTIX_LIME = new Color(119, 211, 27);
    private static final Color TEXT = new Color(17, 30, 42);
    private static final Color MUTED = new Color(91, 108, 120);
    private static final Color BORDER = new Color(219, 228, 232);
    private static final Color SURFACE = new Color(247, 250, 249);
    private static final Color SOFT_GREEN = new Color(230, 246, 239);
    private static final Color WHITE = Color.WHITE;

    public byte[] toPdf(ReportDataset report) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ReportWriter writer = new ReportWriter(document, report);
            writer.render();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF.",
                    exception);
        }
    }

    private static final class ReportWriter {

        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 34;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
        private static final float TABLE_BOTTOM = 72;

        private final PDDocument document;
        private final ReportDataset report;
        private final PDType1Font regular = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA_BOLD);

        private PDPage page;
        private PDPageContentStream content;
        private int pageNumber;
        private float y;

        private ReportWriter(
                PDDocument document,
                ReportDataset report) {
            this.document = document;
            this.report = report;
        }

        private void render() throws IOException {
            newPage(true);
            drawExecutiveSummary();
            drawEventTable();
            drawMonthlyRevenue();
            closePage();
        }

        private void newPage(boolean coverHeader) throws IOException {
            closePage();
            pageNumber++;
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            fillRect(WHITE, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            drawHeader(coverHeader);
            drawFooter();
            y = coverHeader ? 650 : 718;
        }

        private void drawHeader(boolean coverHeader) throws IOException {
            float height = coverHeader ? 154 : 82;
            float bottom = PAGE_HEIGHT - height;
            fillRect(NAVY, 0, bottom, PAGE_WIDTH, height);
            fillRect(DARK_GREEN, 0, bottom, 164, height);
            fillRect(EVENTIX_LIME, 153, bottom, 7, height);

            write("E", 38, PAGE_HEIGHT - 58, 42, true, EVENTIX_GREEN);
            write("Event", 71, PAGE_HEIGHT - 56, 25, true, WHITE);
            write("ix", 139, PAGE_HEIGHT - 56, 25, true, EVENTIX_LIME);

            if (coverHeader) {
                write("REPORTE EJECUTIVO", 198, PAGE_HEIGHT - 49,
                        10, true, new Color(196, 235, 217));
                write("Resultados y rendimiento", 198, PAGE_HEIGHT - 83,
                        24, true, WHITE);
                write("Visión consolidada de ventas, entradas, reservaciones y asistencia.",
                        198, PAGE_HEIGHT - 106, 9, false,
                        new Color(213, 225, 229));
                write("Período " + report.from().format(DATE)
                                + " - " + report.to().format(DATE),
                        198, PAGE_HEIGHT - 129, 9, true, EVENTIX_GREEN);
            } else {
                write("REPORTE EJECUTIVO", 198, PAGE_HEIGHT - 49,
                        12, true, WHITE);
                write(report.from().format(DATE) + " - "
                                + report.to().format(DATE),
                        198, PAGE_HEIGHT - 66, 8, false,
                        new Color(196, 235, 217));
            }
        }

        private void drawExecutiveSummary() throws IOException {
            sectionTitle("RESUMEN EJECUTIVO", y);
            y -= 34;

            ReportSummary summary = report.summary();
            float gap = 10;
            float cardWidth = (CONTENT_WIDTH - gap * 3) / 4;
            float cardHeight = 74;

            metricCard(MARGIN, y - cardHeight, cardWidth, cardHeight,
                    "INGRESOS",
                    "DOP " + decimal(summary.revenue()),
                    "Ventas pagadas");
            metricCard(MARGIN + cardWidth + gap, y - cardHeight,
                    cardWidth, cardHeight,
                    "VENTAS",
                    Long.toString(summary.sales()),
                    "Transacciones pagadas");
            metricCard(MARGIN + (cardWidth + gap) * 2, y - cardHeight,
                    cardWidth, cardHeight,
                    "ENTRADAS",
                    Long.toString(summary.ticketsSold()),
                    "Boletas vendidas");
            metricCard(MARGIN + (cardWidth + gap) * 3, y - cardHeight,
                    cardWidth, cardHeight,
                    "ASISTENTES",
                    Long.toString(summary.attendees()),
                    "Primeros accesos");

            y -= cardHeight + 12;

            metricCard(MARGIN, y - cardHeight, cardWidth, cardHeight,
                    "RESERVACIONES",
                    Long.toString(summary.reservations()),
                    summary.reservedPlaces() + " plazas reservadas");
            metricCard(MARGIN + cardWidth + gap, y - cardHeight,
                    cardWidth, cardHeight,
                    "CONVERSIÓN",
                    decimal(summary.conversionRate()) + "%",
                    "Ventas / reservaciones");
            metricCard(MARGIN + (cardWidth + gap) * 2, y - cardHeight,
                    cardWidth, cardHeight,
                    "ASISTENCIA",
                    decimal(summary.attendanceRate()) + "%",
                    "Asistentes / entradas");
            metricCard(MARGIN + (cardWidth + gap) * 3, y - cardHeight,
                    cardWidth, cardHeight,
                    "EVENTOS",
                    Long.toString(report.byEvent().size()),
                    "En el alcance del reporte");

            y -= cardHeight + 28;
        }

        private void metricCard(
                float x,
                float bottom,
                float width,
                float height,
                String label,
                String value,
                String note) throws IOException {
            fillRect(WHITE, x, bottom, width, height);
            strokeRect(BORDER, x, bottom, width, height, 0.8f);
            fillRect(EVENTIX_GREEN, x, bottom + height - 4, width, 4);
            write(label, x + 12, bottom + height - 23,
                    7, true, DARK_GREEN);
            write(truncate(value, 19), x + 12, bottom + 28,
                    value.length() > 15 ? 13 : 17, true, TEXT);
            write(truncate(note, 28), x + 12, bottom + 11,
                    6.8f, false, MUTED);
        }

        private void drawEventTable() throws IOException {
            sectionTitle("RESULTADOS POR EVENTO", y);
            y -= 27;
            drawEventTableHeader();

            if (report.byEvent().isEmpty()) {
                fillRect(SURFACE, MARGIN, y - 46, CONTENT_WIDTH, 46);
                write("No hay datos para el período seleccionado.",
                        MARGIN + 14, y - 28, 9, false, MUTED);
                y -= 62;
                return;
            }

            for (EventReportRow row : report.byEvent()) {
                ensureEventRowSpace();
                drawEventRow(row);
            }
            y -= 18;
        }

        private void drawEventTableHeader() throws IOException {
            float height = 28;
            fillRect(NAVY, MARGIN, y - height, CONTENT_WIDTH, height);
            write("EVENTO", MARGIN + 10, y - 18, 7, true, WHITE);
            write("VENTAS", 336, y - 18, 7, true, WHITE);
            write("ENTRADAS", 382, y - 18, 7, true, WHITE);
            write("ASIST.", 439, y - 18, 7, true, WHITE);
            write("INGRESOS", 485, y - 18, 7, true, WHITE);
            y -= height;
        }

        private void drawEventRow(EventReportRow row) throws IOException {
            float height = 49;
            fillRect(WHITE, MARGIN, y - height, CONTENT_WIDTH, height);
            strokeRect(BORDER, MARGIN, y - height, CONTENT_WIDTH, height, 0.5f);

            write(truncate(row.eventName(), 42),
                    MARGIN + 10, y - 18, 8.5f, true, TEXT);
            write(truncate(row.categoryName() + " - " + row.organizerName(), 56),
                    MARGIN + 10, y - 34, 6.5f, false, MUTED);

            write(Long.toString(row.sales()), 348, y - 25,
                    8, true, TEXT);
            write(Long.toString(row.ticketsSold()), 398, y - 25,
                    8, true, TEXT);
            write(Long.toString(row.attendees()), 450, y - 25,
                    8, true, TEXT);
            write("DOP " + decimal(row.revenue()), 485, y - 25,
                    7.5f, true, DARK_GREEN);
            y -= height;
        }

        private void ensureEventRowSpace() throws IOException {
            if (y - 52 < TABLE_BOTTOM) {
                newPage(false);
                sectionTitle("RESULTADOS POR EVENTO - CONTINUACIÓN", y);
                y -= 27;
                drawEventTableHeader();
            }
        }

        private void drawMonthlyRevenue() throws IOException {
            if (report.monthlyRevenue().isEmpty()) {
                return;
            }
            if (y < 250) {
                newPage(false);
            }

            sectionTitle("EVOLUCIÓN DE INGRESOS", y);
            y -= 30;

            BigDecimal maximum = report.monthlyRevenue().stream()
                    .map(MonthlyRevenueRow::revenue)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            int visible = Math.min(report.monthlyRevenue().size(), 8);
            List<MonthlyRevenueRow> rows = report.monthlyRevenue()
                    .subList(Math.max(0, report.monthlyRevenue().size() - visible),
                            report.monthlyRevenue().size());

            for (MonthlyRevenueRow row : rows) {
                ensureMonthlyRowSpace();
                drawMonthlyRow(row, maximum);
            }
        }

        private void drawMonthlyRow(
                MonthlyRevenueRow row,
                BigDecimal maximum) throws IOException {
            float rowHeight = 39;
            float barX = 184;
            float barWidth = 250;
            float ratio = maximum.signum() == 0
                    ? 0
                    : row.revenue().divide(maximum, 4,
                            java.math.RoundingMode.HALF_UP).floatValue();

            write(truncate(row.period(), 19), MARGIN, y - 15,
                    8, true, TEXT);
            write(row.sales() + " ventas / " + row.ticketsSold() + " entradas",
                    MARGIN, y - 29, 6.5f, false, MUTED);

            fillRect(SURFACE, barX, y - 25, barWidth, 13);
            if (ratio > 0) {
                fillRect(EVENTIX_GREEN, barX, y - 25,
                        Math.max(3, barWidth * ratio), 13);
            }
            write("DOP " + decimal(row.revenue()), 451, y - 22,
                    7.5f, true, DARK_GREEN);
            y -= rowHeight;
        }

        private void ensureMonthlyRowSpace() throws IOException {
            if (y - 42 < TABLE_BOTTOM) {
                newPage(false);
                sectionTitle("EVOLUCIÓN DE INGRESOS - CONTINUACIÓN", y);
                y -= 30;
            }
        }

        private void sectionTitle(String value, float top) throws IOException {
            fillRect(EVENTIX_GREEN, MARGIN, top - 4, 22, 4);
            write(value, MARGIN + 31, top - 8, 10, true, NAVY);
        }

        private void drawFooter() throws IOException {
            fillRect(SURFACE, 0, 0, PAGE_WIDTH, 48);
            write("Eventix - Reporte generado automáticamente",
                    MARGIN, 22, 7, false, MUTED);
            write("Página " + pageNumber,
                    PAGE_WIDTH - 72, 22, 7, true, DARK_GREEN);
        }

        private void fillRect(
                Color color,
                float x,
                float bottom,
                float width,
                float height) throws IOException {
            content.setNonStrokingColor(color);
            content.addRect(x, bottom, width, height);
            content.fill();
        }

        private void strokeRect(
                Color color,
                float x,
                float bottom,
                float width,
                float height,
                float lineWidth) throws IOException {
            content.setStrokingColor(color);
            content.setLineWidth(lineWidth);
            content.addRect(x, bottom, width, height);
            content.stroke();
        }

        private void write(
                String value,
                float x,
                float baseline,
                float size,
                boolean isBold,
                Color color) throws IOException {
            content.beginText();
            content.setFont(isBold ? bold : regular, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, baseline);
            content.showText(sanitize(value));
            content.endText();
        }

        private String sanitize(String value) {
            if (value == null) {
                return "";
            }
            return value
                    .replace('–', '-')
                    .replace('—', '-')
                    .replace('·', '-')
                    .replaceAll("[^\\x20-\\x7EÀ-ÿ]", "-");
        }

        private String truncate(String value, int maximum) {
            String safe = value == null ? "" : value;
            return safe.length() <= maximum
                    ? safe
                    : safe.substring(0, maximum - 3) + "...";
        }

        private String decimal(BigDecimal value) {
            return String.format(REPORT_LOCALE, "%.2f", value);
        }

        private void closePage() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }
    }
}
