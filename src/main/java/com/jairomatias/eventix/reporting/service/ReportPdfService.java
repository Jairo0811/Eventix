package com.jairomatias.eventix.reporting.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportSummary;

@Service
public class ReportPdfService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale REPORT_LOCALE = Locale.of("es", "DO");

    private static final Color NAVY = new Color(4, 32, 43);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color TEAL = new Color(5, 150, 105);
    private static final Color TEXT = new Color(20, 32, 37);
    private static final Color MUTED = new Color(93, 108, 113);
    private static final Color BORDER = new Color(220, 228, 226);
    private static final Color SOFT = new Color(246, 249, 248);
    private static final Color LIGHT_GREEN = new Color(232, 247, 238);

    public byte[] create(ReportDataset report) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.openPage(report);
            writer.summary(report.summary());
            writer.sectionTitle("RESULTADOS POR EVENTO",
                    "Desempeño comercial y asistencia por actividad");

            if (report.byEvent().isEmpty()) {
                writer.emptyState("No hay eventos con datos para el período seleccionado.");
            } else {
                for (EventReportRow row : report.byEvent()) {
                    writer.eventCard(row, report);
                }
            }

            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF.", exception);
        }
    }

    private static final class PdfWriter {
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 38;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);

        private final PDDocument document;
        private final PDType1Font regular = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA_BOLD);

        private PDPageContentStream content;
        private float y;
        private int pageNumber;

        private PdfWriter(PDDocument document) {
            this.document = document;
        }

        private void openPage(ReportDataset report) throws IOException {
            closeCurrentPage();
            pageNumber++;
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);

            fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, SOFT);
            fillRect(24, 24, PAGE_WIDTH - 48, PAGE_HEIGHT - 48, Color.WHITE);
            header(report);
            y = 665;
        }

        private void header(ReportDataset report) throws IOException {
            fillRect(24, 716, PAGE_WIDTH - 48, 102, NAVY);
            fillRect(24, 716, 8, 102, GREEN);
            fillRect(44, 760, 32, 32, GREEN);
            text("E", 54, 768, 18, true, Color.WHITE);
            text("Eventix", 88, 772, 22, true, Color.WHITE);
            text("REPORTE EJECUTIVO", 88, 752, 9, true,
                    new Color(185, 232, 211));

            text("PERÍODO ANALIZADO", 386, 777, 8, true,
                    new Color(185, 232, 211));
            text(report.from().format(DATE) + " - " + report.to().format(DATE),
                    386, 758, 10, true, Color.WHITE);
            text("Generado " + LocalDateTime.now().format(GENERATED_AT),
                    386, 740, 8, false, Color.WHITE);
        }

        private void summary(ReportSummary summary) throws IOException {
            text("RESUMEN GENERAL", MARGIN, y, 11, true, TEAL);
            y -= 19;
            text("Indicadores principales del período seleccionado",
                    MARGIN, y, 9, false, MUTED);
            y -= 27;

            float gap = 10;
            float cardWidth = (CONTENT_WIDTH - (gap * 2)) / 3;
            metricCard(MARGIN, y - 70, cardWidth, 70,
                    "INGRESOS", money(summary.revenue()), GREEN);
            metricCard(MARGIN + cardWidth + gap, y - 70, cardWidth, 70,
                    "VENTAS", Long.toString(summary.sales()), TEAL);
            metricCard(MARGIN + ((cardWidth + gap) * 2), y - 70, cardWidth, 70,
                    "ENTRADAS", Long.toString(summary.ticketsSold()), NAVY);
            y -= 84;

            float smallWidth = (CONTENT_WIDTH - (gap * 3)) / 4;
            metricCard(MARGIN, y - 58, smallWidth, 58,
                    "RESERVACIONES", Long.toString(summary.reservations()), TEAL);
            metricCard(MARGIN + smallWidth + gap, y - 58, smallWidth, 58,
                    "ASISTENTES", Long.toString(summary.attendees()), GREEN);
            metricCard(MARGIN + ((smallWidth + gap) * 2), y - 58, smallWidth, 58,
                    "CONVERSIÓN", percent(summary.conversionRate()), NAVY);
            metricCard(MARGIN + ((smallWidth + gap) * 3), y - 58, smallWidth, 58,
                    "ASISTENCIA", percent(summary.attendanceRate()), NAVY);
            y -= 82;
        }

        private void metricCard(
                float x,
                float bottom,
                float width,
                float height,
                String label,
                String value,
                Color accent) throws IOException {
            card(x, bottom, width, height, Color.WHITE);
            fillRect(x, bottom, 5, height, accent);
            text(label, x + 16, bottom + height - 21, 7, true, MUTED);
            text(truncate(value, 24), x + 16, bottom + 17,
                    height >= 68 ? 17 : 13, true, TEXT);
        }

        private void sectionTitle(String title, String subtitle) throws IOException {
            ensureSpace(52, null);
            text(title, MARGIN, y, 11, true, TEAL);
            y -= 18;
            text(subtitle, MARGIN, y, 9, false, MUTED);
            y -= 22;
        }

        private void eventCard(EventReportRow row, ReportDataset report)
                throws IOException {
            ensureSpace(112, report);
            float bottom = y - 100;
            card(MARGIN, bottom, CONTENT_WIDTH, 100, Color.WHITE);
            fillRect(MARGIN, bottom, 5, 100, GREEN);

            text(truncate(row.eventName(), 46), MARGIN + 17, y - 21,
                    12, true, TEXT);
            text(truncate(row.categoryName(), 28) + "  |  "
                    + truncate(row.organizerName(), 32),
                    MARGIN + 17, y - 39, 8, false, MUTED);

            float metricY = y - 68;
            eventMetric(MARGIN + 17, metricY, "INGRESOS", money(row.revenue()));
            eventMetric(MARGIN + 150, metricY, "VENTAS", Long.toString(row.sales()));
            eventMetric(MARGIN + 240, metricY, "ENTRADAS", Long.toString(row.ticketsSold()));
            eventMetric(MARGIN + 337, metricY, "RESERVAS", Long.toString(row.reservations()));
            eventMetric(MARGIN + 431, metricY, "ASISTENTES", Long.toString(row.attendees()));

            y = bottom - 12;
        }

        private void eventMetric(float x, float top, String label, String value)
                throws IOException {
            text(label, x, top, 6.5f, true, MUTED);
            text(truncate(value, 18), x, top - 17, 9, true, TEXT);
        }

        private void emptyState(String message) throws IOException {
            card(MARGIN, y - 72, CONTENT_WIDTH, 72, LIGHT_GREEN);
            text("SIN DATOS", MARGIN + 18, y - 27, 10, true, GREEN);
            text(message, MARGIN + 18, y - 48, 9, false, MUTED);
            y -= 86;
        }

        private void ensureSpace(float needed, ReportDataset report) throws IOException {
            if (y - needed >= 72) {
                return;
            }
            if (report == null) {
                return;
            }
            openPage(report);
            text("RESULTADOS POR EVENTO - CONTINUACIÓN", MARGIN, y,
                    10, true, TEAL);
            y -= 28;
        }

        private void footer() throws IOException {
            content.setStrokingColor(BORDER);
            content.moveTo(MARGIN, 58);
            content.lineTo(PAGE_WIDTH - MARGIN, 58);
            content.stroke();
            text("Eventix · Gestión profesional de eventos, ventas y accesos",
                    MARGIN, 41, 7.5f, false, MUTED);
            text("Página " + pageNumber,
                    PAGE_WIDTH - 78, 41, 7.5f, true, MUTED);
        }

        private void close() throws IOException {
            closeCurrentPage();
        }

        private void closeCurrentPage() throws IOException {
            if (content != null) {
                footer();
                content.close();
                content = null;
            }
        }

        private void card(
                float x,
                float y,
                float width,
                float height,
                Color fill) throws IOException {
            content.setNonStrokingColor(fill);
            content.setStrokingColor(BORDER);
            content.addRect(x, y, width, height);
            content.fillAndStroke();
        }

        private void fillRect(
                float x,
                float y,
                float width,
                float height,
                Color color) throws IOException {
            content.setNonStrokingColor(color);
            content.addRect(x, y, width, height);
            content.fill();
        }

        private void text(
                String value,
                float x,
                float y,
                float size,
                boolean isBold,
                Color color) throws IOException {
            content.beginText();
            content.setFont(isBold ? bold : regular, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, y);
            content.showText(pdfSafe(value));
            content.endText();
        }

        private String truncate(String value, int maximum) {
            if (value == null) {
                return "";
            }
            return value.length() <= maximum
                    ? value
                    : value.substring(0, maximum - 3) + "...";
        }

        private String money(BigDecimal value) {
            return String.format(REPORT_LOCALE, "RD$ %,.2f", value);
        }

        private String percent(BigDecimal value) {
            return String.format(REPORT_LOCALE, "%.1f%%", value);
        }

        private String pdfSafe(String value) {
            return value == null
                    ? ""
                    : value.replace("·", "-")
                            .replaceAll("[^\\x20-\\x7EÀ-ÿ]", "-");
        }
    }
}
