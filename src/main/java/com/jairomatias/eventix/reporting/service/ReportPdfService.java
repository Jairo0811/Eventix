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
import com.jairomatias.eventix.shared.pdf.PdfBranding;

@Service
public class ReportPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale REPORT_LOCALE = Locale.of("es", "DO");

    private static final Color NAVY = new Color(4, 32, 43);
    private static final Color NAVY_SOFT = new Color(8, 55, 66);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color TEAL = new Color(5, 150, 105);
    private static final Color TEXT = new Color(20, 32, 37);
    private static final Color MUTED = new Color(93, 108, 113);
    private static final Color BORDER = new Color(220, 228, 226);
    private static final Color SOFT = new Color(246, 249, 248);
    private static final Color LIGHT_GREEN = new Color(236, 249, 242);
    private static final Color PALE_GREEN = new Color(246, 252, 248);

    public byte[] create(ReportDataset report) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.openPage(report);
            writer.summary(report.summary());
            writer.sectionTitle("RESULTADOS POR EVENTO", "Desempeño comercial y asistencia por actividad");

            if (report.byEvent().isEmpty()) {
                writer.emptyState("No hay eventos con datos para el período seleccionado.");
            } else {
                for (EventReportRow row : report.byEvent()) {
                    writer.eventCard(row, report);
                }
            }

            writer.quickAnalysis(report.summary(), report);
            writer.verificationBand(report);
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el reporte PDF.", exception);
        }
    }

    private static final class PdfWriter {
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 38;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);

        private final PDDocument document;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

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
            fillRect(16, 16, PAGE_WIDTH - 32, PAGE_HEIGHT - 32, Color.WHITE);
            header(report);
            y = 647;
        }

        private void header(ReportDataset report) throws IOException {
            fillRect(16, 691, PAGE_WIDTH - 32, 135, NAVY);
            fillRect(16, 691, 7, 135, GREEN);
            PdfBranding.drawOfficialLogo(document, content, 40, 727, 116, 78);
            verticalLine(174, 720, 800, new Color(56, 93, 100));

            text("REPORTE", 194, 778, 22, true, Color.WHITE);
            text("EJECUTIVO", 194, 746, 22, true, new Color(38, 210, 117));
            fillRect(194, 728, 34, 3, GREEN);
            text("Inteligencia operativa Eventix", 194, 709, 8.5f, false, Color.WHITE);

            infoLabel("PERÍODO ANALIZADO", report.from().format(DATE) + " - " + report.to().format(DATE), 423, 786);
            infoLabel("GENERADO", LocalDateTime.now().format(GENERATED_AT), 423, 748);
            infoLabel("TIPO DE REPORTE", "Resumen ejecutivo", 423, 710);
        }

        private void infoLabel(String label, String value, float x, float top) throws IOException {
            text(label, x, top, 6.5f, true, new Color(185, 232, 211));
            text(value, x, top - 15, 8.5f, true, Color.WHITE);
        }

        private void summary(ReportSummary summary) throws IOException {
            sectionTitle("RESUMEN GENERAL", "Indicadores principales del período seleccionado");

            float gap = 12;
            float cardWidth = (CONTENT_WIDTH - (gap * 2)) / 3;
            metricCard(MARGIN, y - 76, cardWidth, 76, "INGRESOS", money(summary.revenue()), "Total del período", GREEN);
            metricCard(MARGIN + cardWidth + gap, y - 76, cardWidth, 76, "VENTAS", Long.toString(summary.sales()), "Total de ventas", GREEN);
            metricCard(MARGIN + ((cardWidth + gap) * 2), y - 76, cardWidth, 76, "ENTRADAS", Long.toString(summary.ticketsSold()), "Entradas emitidas", NAVY);
            y -= 91;

            float smallWidth = (CONTENT_WIDTH - (gap * 3)) / 4;
            metricCard(MARGIN, y - 66, smallWidth, 66, "RESERVACIONES", Long.toString(summary.reservations()), "Reservas realizadas", GREEN);
            metricCard(MARGIN + smallWidth + gap, y - 66, smallWidth, 66,
                    "ASISTENTES", Long.toString(summary.attendees()), "Asistencias registradas", GREEN);
            metricCard(MARGIN + ((smallWidth + gap) * 2), y - 66, smallWidth, 66,
                    "CONVERSIÓN", percent(summary.conversionRate()), "Ventas / Reservaciones", NAVY);
            metricCard(MARGIN + ((smallWidth + gap) * 3), y - 66, smallWidth, 66,
                    "ASISTENCIA", percent(summary.attendanceRate()), "Asistentes / Entradas", NAVY);
            y -= 87;
        }

        private void metricCard(float x, float bottom, float width, float height,
                String label, String value, String caption, Color accent) throws IOException {
            card(x, bottom, width, height, Color.WHITE);
            fillRect(x, bottom, 4, height, accent);
            circle(x + 24, bottom + height - 25, 12, accent);
            text(metricGlyph(label), x + 20, bottom + height - 29, 9, true, Color.WHITE);
            text(label, x + 45, bottom + height - 22, 7, true, TEXT);
            text(truncate(value, 22), x + 45, bottom + height - 44, height >= 74 ? 15 : 12, true,
                    "INGRESOS".equals(label) ? new Color(15, 124, 67) : TEXT);
            text(truncate(caption, 28), x + 45, bottom + 10, 6.3f, false, MUTED);
        }

        private String metricGlyph(String label) {
            return switch (label) {
                case "INGRESOS" -> "$";
                case "VENTAS" -> "V";
                case "ENTRADAS" -> "T";
                case "RESERVACIONES" -> "R";
                case "ASISTENTES" -> "A";
                case "CONVERSIÓN" -> "%";
                case "ASISTENCIA" -> "%";
                default -> "•";
            };
        }

        private void sectionTitle(String title, String subtitle) throws IOException {
            text(title, MARGIN, y, 13, true, NAVY);
            fillRect(MARGIN, y - 17, 24, 2.5f, GREEN);
            text(subtitle, MARGIN + 38, y - 18, 8.2f, false, MUTED);
            y -= 38;
        }

        private void eventCard(EventReportRow row, ReportDataset report) throws IOException {
            ensureSpace(128, report);
            float bottom = y - 112;
            card(MARGIN, bottom, CONTENT_WIDTH, 112, PALE_GREEN);
            fillRect(MARGIN, y - 48, CONTENT_WIDTH, 48, LIGHT_GREEN);
            circle(MARGIN + 25, y - 24, 13, NAVY);
            text("E", MARGIN + 21, y - 28, 9, true, Color.WHITE);
            text(truncate(row.eventName(), 42), MARGIN + 50, y - 21, 13, true, NAVY);
            text(truncate(row.categoryName(), 25) + "  |  " + truncate(row.organizerName(), 34),
                    MARGIN + 50, y - 38, 7.5f, false, MUTED);

            float metricTop = y - 67;
            float column = CONTENT_WIDTH / 5;
            eventMetric(MARGIN + 18, metricTop, "INGRESOS", money(row.revenue()), GREEN);
            eventMetric(MARGIN + column + 10, metricTop, "VENTAS", Long.toString(row.sales()), TEXT);
            eventMetric(MARGIN + (column * 2) + 7, metricTop, "ENTRADAS", Long.toString(row.ticketsSold()), TEXT);
            eventMetric(MARGIN + (column * 3) + 4, metricTop, "RESERVAS", Long.toString(row.reservations()), TEXT);
            eventMetric(MARGIN + (column * 4), metricTop, "ASISTENTES", Long.toString(row.attendees()), TEXT);
            y = bottom - 14;
        }

        private void eventMetric(float x, float top, String label, String value, Color valueColor) throws IOException {
            text(label, x, top, 6.2f, true, MUTED);
            text(truncate(value, 17), x, top - 20, 9.5f, true, valueColor);
        }

        private void quickAnalysis(ReportSummary summary, ReportDataset report) throws IOException {
            ensureSpace(108, report);
            float bottom = y - 94;
            card(MARGIN, bottom, CONTENT_WIDTH, 94, LIGHT_GREEN);
            fillRect(MARGIN, bottom, 70, 94, new Color(231, 248, 238));
            circle(MARGIN + 35, bottom + 47, 18, GREEN);
            text("↗", MARGIN + 27, bottom + 40, 18, true, Color.WHITE);
            text("ANÁLISIS RÁPIDO", MARGIN + 88, y - 23, 10, true, new Color(15, 124, 67));

            float lineY = y - 45;
            analysisLine(MARGIN + 88, lineY, conversionInsight(summary));
            analysisLine(MARGIN + 88, lineY - 18, attendanceInsight(summary));
            analysisLine(MARGIN + 88, lineY - 36, recommendation(summary));
            y = bottom - 13;
        }

        private String conversionInsight(ReportSummary summary) {
            if (summary.reservations() == 0) {
                return "Aún no hay reservaciones suficientes para medir la conversión.";
            }
            if (summary.conversionRate().compareTo(new BigDecimal("75")) >= 0) {
                return "Excelente conversión: " + percent(summary.conversionRate()) + " de las reservaciones generaron ventas.";
            }
            return "Conversión actual: " + percent(summary.conversionRate()) + ". Hay margen para optimizar el cierre de reservas.";
        }

        private String attendanceInsight(ReportSummary summary) {
            if (summary.attendees() == 0) {
                return "Aún no hay asistencias registradas para el período analizado.";
            }
            return "La tasa de asistencia registrada es de " + percent(summary.attendanceRate()) + ".";
        }

        private String recommendation(ReportSummary summary) {
            if (summary.ticketsSold() > 0 && summary.attendees() == 0) {
                return "Revisa promoción, recordatorios y control de acceso para mejorar la asistencia.";
            }
            if (summary.sales() == 0) {
                return "Impulsa la promoción de eventos y monitorea el embudo de reservación.";
            }
            return "Mantén el seguimiento comercial y operativo para sostener los resultados del período.";
        }

        private void analysisLine(float x, float baseline, String value) throws IOException {
            circle(x + 5, baseline + 2, 5, TEAL);
            text("✓", x + 2, baseline - 1, 6, true, Color.WHITE);
            text(truncate(value, 78), x + 17, baseline, 7.3f, false, TEXT);
        }

        private void verificationBand(ReportDataset report) throws IOException {
            ensureSpace(70, report);
            float bottom = y - 58;
            fillRect(MARGIN, bottom, CONTENT_WIDTH, 58, NAVY);
            circle(MARGIN + 26, bottom + 29, 14, GREEN);
            text("✓", MARGIN + 21, bottom + 24, 11, true, Color.WHITE);
            text("EVENTIX", MARGIN + 48, bottom + 34, 8, true, Color.WHITE);
            text("VERIFICADO", MARGIN + 48, bottom + 20, 6.5f, false, new Color(185, 232, 211));

            bandItem(MARGIN + 155, bottom, "FUENTE DE DATOS", "Plataforma Eventix");
            bandItem(MARGIN + 285, bottom, "REPORTE GENERADO POR", "Eventix - Sistema de Reportes");
            bandItem(MARGIN + 430, bottom, "DOCUMENTO", "Resumen ejecutivo");
            y = bottom - 12;
        }

        private void bandItem(float x, float bottom, String label, String value) throws IOException {
            text(label, x, bottom + 34, 5.8f, true, new Color(185, 232, 211));
            text(truncate(value, 28), x, bottom + 19, 6.7f, false, Color.WHITE);
        }

        private void emptyState(String message) throws IOException {
            card(MARGIN, y - 72, CONTENT_WIDTH, 72, LIGHT_GREEN);
            text("SIN DATOS", MARGIN + 18, y - 27, 10, true, GREEN);
            text(message, MARGIN + 18, y - 48, 9, false, MUTED);
            y -= 86;
        }

        private void ensureSpace(float needed, ReportDataset report) throws IOException {
            if (y - needed >= 76) {
                return;
            }
            if (report == null) {
                return;
            }
            openPage(report);
            text("CONTINUACIÓN DEL REPORTE", MARGIN, y, 10, true, TEAL);
            y -= 28;
        }

        private void footer() throws IOException {
            content.setStrokingColor(BORDER);
            content.moveTo(MARGIN, 50);
            content.lineTo(PAGE_WIDTH - MARGIN, 50);
            content.stroke();
            text("Eventix · Reporte ejecutivo", MARGIN, 34, 7, false, MUTED);
            text("Página " + pageNumber, PAGE_WIDTH - MARGIN - 45, 34, 7, false, MUTED);
        }

        private void close() throws IOException {
            closeCurrentPage();
        }

        private void closeCurrentPage() throws IOException {
            if (content == null) {
                return;
            }
            footer();
            content.close();
            content = null;
        }

        private void card(float x, float y, float width, float height, Color fill) throws IOException {
            fillRect(x, y, width, height, fill);
            content.setStrokingColor(BORDER);
            content.addRect(x, y, width, height);
            content.stroke();
        }

        private void fillRect(float x, float y, float width, float height, Color color) throws IOException {
            content.setNonStrokingColor(color);
            content.addRect(x, y, width, height);
            content.fill();
        }

        private void circle(float cx, float cy, float radius, Color color) throws IOException {
            float k = 0.552284749831f;
            content.setNonStrokingColor(color);
            content.moveTo(cx + radius, cy);
            content.curveTo(cx + radius, cy + (k * radius), cx + (k * radius), cy + radius, cx, cy + radius);
            content.curveTo(cx - (k * radius), cy + radius, cx - radius, cy + (k * radius), cx - radius, cy);
            content.curveTo(cx - radius, cy - (k * radius), cx - (k * radius), cy - radius, cx, cy - radius);
            content.curveTo(cx + (k * radius), cy - radius, cx + radius, cy - (k * radius), cx + radius, cy);
            content.fill();
        }

        private void verticalLine(float x, float bottom, float top, Color color) throws IOException {
            content.setStrokingColor(color);
            content.moveTo(x, bottom);
            content.lineTo(x, top);
            content.stroke();
        }

        private void text(String value, float x, float y, float size, boolean boldText, Color color) throws IOException {
            content.beginText();
            content.setFont(boldText ? bold : regular, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, y);
            content.showText(sanitize(value));
            content.endText();
        }

        private String money(BigDecimal value) {
            return String.format(REPORT_LOCALE, "RD$ %,.2f", value);
        }

        private String percent(BigDecimal value) {
            return String.format(REPORT_LOCALE, "%.2f%%", value);
        }

        private String truncate(String value, int maxLength) {
            if (value == null || value.isBlank()) {
                return "-";
            }
            if (value.length() <= maxLength) {
                return value;
            }
            return value.substring(0, Math.max(1, maxLength - 3)) + "...";
        }

        private String sanitize(String value) {
            return value
                    .replace("↗", "+")
                    .replace("✓", "OK")
                    .replace("•", "-")
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace("’", "'")
                    .replace("“", "\"")
                    .replace("”", "\"");
        }
    }
}
