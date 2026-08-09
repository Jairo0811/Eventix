package com.jairomatias.eventix.reporting.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;

@Service
public class ReportExportService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale REPORT_LOCALE = Locale.of("es", "DO");
    private static final Color EVENTIX_GREEN = new Color(21, 128, 61);

    public byte[] toCsv(ReportDataset report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Reporte Eventix;")
                .append(report.from().format(DATE))
                .append(" - ")
                .append(report.to().format(DATE))
                .append("\r\n\r\n");
        csv.append("Ingresos;Ventas;Entradas vendidas;Reservaciones;"
                + "Asistentes;Conversión;Asistencia\r\n");
        csv.append(decimal(report.summary().revenue())).append(';')
                .append(report.summary().sales()).append(';')
                .append(report.summary().ticketsSold()).append(';')
                .append(report.summary().reservations()).append(';')
                .append(report.summary().attendees()).append(';')
                .append(decimal(report.summary().conversionRate())).append("%;")
                .append(decimal(report.summary().attendanceRate())).append("%\r\n\r\n");
        csv.append("Evento;Categoría;Organizador;Ventas;Entradas;"
                + "Reservaciones;Asistentes;Ingresos\r\n");
        report.byEvent().forEach(row -> csv
                .append(csvCell(row.eventName())).append(';')
                .append(csvCell(row.categoryName())).append(';')
                .append(csvCell(row.organizerName())).append(';')
                .append(row.sales()).append(';')
                .append(row.ticketsSold()).append(';')
                .append(row.reservations()).append(';')
                .append(row.attendees()).append(';')
                .append(decimal(row.revenue())).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toXlsx(ReportDataset report) {
        List<List<Cell>> rows = workbookRows(report);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            addZipEntry(zip, "[Content_Types].xml", contentTypes());
            addZipEntry(zip, "_rels/.rels", rootRelationships());
            addZipEntry(zip, "docProps/app.xml", appProperties());
            addZipEntry(zip, "docProps/core.xml", coreProperties());
            addZipEntry(zip, "xl/workbook.xml", workbook());
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
            addZipEntry(zip, "xl/styles.xml", styles());
            addZipEntry(zip, "xl/worksheets/sheet1.xml", worksheet(rows));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte Excel.", exception);
        }
    }

    public byte[] toPdf(ReportDataset report) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfPageWriter writer = new PdfPageWriter(document, regular, bold);
            writer.title("Reporte ejecutivo Eventix");
            writer.text("Período: " + report.from().format(DATE)
                    + " - " + report.to().format(DATE));
            writer.space(8);
            writer.heading("Resumen");
            writer.text("Ingresos: DOP " + decimal(report.summary().revenue()));
            writer.text("Ventas: " + report.summary().sales()
                    + "   Entradas: " + report.summary().ticketsSold()
                    + "   Asistentes: " + report.summary().attendees());
            writer.text("Conversión: "
                    + decimal(report.summary().conversionRate())
                    + "%   Asistencia: "
                    + decimal(report.summary().attendanceRate()) + "%");
            writer.space(10);
            writer.heading("Resultados por evento");
            for (EventReportRow row : report.byEvent()) {
                writer.eventRow(row);
            }
            writer.closePage();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF.", exception);
        }
    }

    private List<List<Cell>> workbookRows(ReportDataset report) {
        List<List<Cell>> rows = new ArrayList<>();
        rows.add(List.of(Cell.header("REPORTE EJECUTIVO EVENTIX")));
        rows.add(List.of(Cell.text("Período"), Cell.text(
                report.from().format(DATE) + " - " + report.to().format(DATE))));
        rows.add(List.of());
        rows.add(List.of(
                Cell.header("Ingresos"),
                Cell.header("Ventas"),
                Cell.header("Entradas"),
                Cell.header("Reservaciones"),
                Cell.header("Asistentes"),
                Cell.header("Conversión %"),
                Cell.header("Asistencia %")));
        rows.add(List.of(
                Cell.money(report.summary().revenue()),
                Cell.number(report.summary().sales()),
                Cell.number(report.summary().ticketsSold()),
                Cell.number(report.summary().reservations()),
                Cell.number(report.summary().attendees()),
                Cell.decimal(report.summary().conversionRate()),
                Cell.decimal(report.summary().attendanceRate())));
        rows.add(List.of());
        rows.add(List.of(Cell.header("RESULTADOS POR EVENTO")));
        rows.add(List.of(
                Cell.header("Evento"),
                Cell.header("Categoría"),
                Cell.header("Organizador"),
                Cell.header("Ventas"),
                Cell.header("Entradas"),
                Cell.header("Reservaciones"),
                Cell.header("Asistentes"),
                Cell.header("Ingresos")));
        report.byEvent().forEach(row -> rows.add(List.of(
                Cell.text(row.eventName()),
                Cell.text(row.categoryName()),
                Cell.text(row.organizerName()),
                Cell.number(row.sales()),
                Cell.number(row.ticketsSold()),
                Cell.number(row.reservations()),
                Cell.number(row.attendees()),
                Cell.money(row.revenue()))));
        rows.add(List.of());
        rows.add(List.of(Cell.header("INGRESOS MENSUALES")));
        rows.add(List.of(
                Cell.header("Período"),
                Cell.header("Ventas"),
                Cell.header("Entradas"),
                Cell.header("Ingresos")));
        report.monthlyRevenue().forEach(row -> rows.add(List.of(
                Cell.text(row.period()),
                Cell.number(row.sales()),
                Cell.number(row.ticketsSold()),
                Cell.money(row.revenue()))));
        return rows;
    }

    private String worksheet(List<List<Cell>> rows) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <cols><col min="1" max="3" width="28" customWidth="1"/><col min="4" max="8" width="16" customWidth="1"/></cols>
                  <sheetData>
                """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<Cell> row = rows.get(rowIndex);
            for (int column = 0; column < row.size(); column++) {
                Cell cell = row.get(column);
                String reference = columnName(column + 1) + (rowIndex + 1);
                if (cell.numeric) {
                    xml.append("<c r=\"").append(reference)
                            .append("\" s=\"").append(cell.style)
                            .append("\"><v>").append(cell.value)
                            .append("</v></c>");
                } else {
                    xml.append("<c r=\"").append(reference)
                            .append("\" t=\"inlineStr\" s=\"")
                            .append(cell.style).append("\"><is><t>")
                            .append(xml(cell.value))
                            .append("</t></is></c>");
                }
            }
            xml.append("</row>");
        }
        xml.append("</sheetData><autoFilter ref=\"A8:H8\"/>"
                + "<pageMargins left=\"0.25\" right=\"0.25\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/>"
                + "</worksheet>");
        return xml.toString();
    }

    private String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="3">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill>
                      <patternFill patternType="solid">
                        <fgColor rgb="FF15803D"/><bgColor indexed="64"/>
                      </patternFill>
                    </fill>
                  </fills>
                  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="4">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
                    <xf numFmtId="4" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                    <xf numFmtId="2" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                  </cellXfs>
                </styleSheet>
                """;
    }

    private String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                </Types>
                """;
    }

    private String rootRelationships() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                    Target="xl/workbook.xml"/>
                  <Relationship Id="rId2"
                    Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"
                    Target="docProps/core.xml"/>
                  <Relationship Id="rId3"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"
                    Target="docProps/app.xml"/>
                </Relationships>
                """;
    }

    private String workbook() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook
                  xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Reporte Eventix" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """;
    }

    private String workbookRelationships() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
                    Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String appProperties() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Eventix</Application></Properties>
                """;
    }

    private String coreProperties() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <cp:coreProperties
                  xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:title>Reporte ejecutivo Eventix</dc:title>
                  <dc:creator>Eventix</dc:creator>
                </cp:coreProperties>
                """;
    }

    private void addZipEntry(ZipOutputStream zip, String path, String value)
            throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int column) {
        StringBuilder name = new StringBuilder();
        int value = column;
        while (value > 0) {
            value--;
            name.insert(0, (char) ('A' + value % 26));
            value /= 26;
        }
        return name.toString();
    }

    private String csvCell(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String decimal(BigDecimal value) {
        return String.format(REPORT_LOCALE, "%.2f", value);
    }

    private String xml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record Cell(String value, boolean numeric, int style) {
        private static Cell text(String value) {
            return new Cell(value, false, 0);
        }

        private static Cell header(String value) {
            return new Cell(value, false, 1);
        }

        private static Cell number(long value) {
            return new Cell(Long.toString(value), true, 0);
        }

        private static Cell decimal(BigDecimal value) {
            return new Cell(value.toPlainString(), true, 3);
        }

        private static Cell money(BigDecimal value) {
            return new Cell(value.toPlainString(), true, 2);
        }
    }

    private static final class PdfPageWriter {
        private static final float MARGIN = 42;
        private static final float LINE = 15;
        private final PDDocument document;
        private final PDType1Font regular;
        private final PDType1Font bold;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private PdfPageWriter(
                PDDocument document,
                PDType1Font regular,
                PDType1Font bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            newPage();
        }

        private void title(String value) throws IOException {
            write(value, bold, 18, EVENTIX_GREEN);
        }

        private void heading(String value) throws IOException {
            ensureSpace(28);
            write(value, bold, 12, EVENTIX_GREEN);
        }

        private void text(String value) throws IOException {
            write(value, regular, 9, Color.DARK_GRAY);
        }

        private void eventRow(EventReportRow row) throws IOException {
            ensureSpace(34);
            write(truncate(row.eventName(), 68), bold, 9, Color.DARK_GRAY);
            write(String.format(
                    REPORT_LOCALE,
                    "%s · %s | Ventas %d | Entradas %d | Asistentes %d | DOP %.2f",
                    row.categoryName(),
                    row.organizerName(),
                    row.sales(),
                    row.ticketsSold(),
                    row.attendees(),
                    row.revenue()), regular, 8, Color.GRAY);
            space(3);
        }

        private void write(
                String value,
                PDType1Font font,
                float size,
                Color color) throws IOException {
            ensureSpace(LINE);
            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(MARGIN, y);
            content.showText(value.replaceAll("[^\\x20-\\x7EÀ-ÿ]", "-"));
            content.endText();
            y -= LINE;
        }

        private void space(float points) {
            y -= points;
        }

        private void ensureSpace(float points) throws IOException {
            if (y - points < MARGIN) {
                closePage();
                newPage();
            }
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void closePage() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        private String truncate(String value, int maximum) {
            return value.length() <= maximum
                    ? value
                    : value.substring(0, maximum - 3) + "...";
        }
    }
}
