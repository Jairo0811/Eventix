package com.jairomatias.eventix.ticket.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

@Service
public class TicketDocumentService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color NAVY = new Color(3, 31, 43);
    private static final Color DARK_GREEN = new Color(0, 92, 68);
    private static final Color EVENTIX_GREEN = new Color(31, 190, 111);
    private static final Color EVENTIX_LIME = new Color(119, 211, 27);
    private static final Color TEXT = new Color(17, 30, 42);
    private static final Color MUTED = new Color(91, 108, 120);
    private static final Color BORDER = new Color(219, 228, 232);
    private static final Color SURFACE = new Color(247, 250, 249);
    private static final Color WARNING = new Color(245, 178, 34);

    private final TicketCryptographyService cryptographyService;

    public TicketDocumentService(
            TicketCryptographyService cryptographyService) {
        this.cryptographyService = cryptographyService;
    }

    public byte[] createQrPng(DigitalTicket ticket) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    cryptographyService.createQrPayload(ticket),
                    BarcodeFormat.QR_CODE,
                    420,
                    420);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el código QR.",
                    exception);
        }
    }

    public byte[] createPdf(DigitalTicket ticket) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject qr = PDImageXObject.createFromByteArray(
                    document,
                    createQrPng(ticket),
                    "eventix-ticket-qr");

            try (PDPageContentStream content =
                    new PDPageContentStream(document, page)) {
                drawBackground(content);
                drawHeader(content, ticket);
                drawAttendeePanel(content, ticket);
                drawQrPanel(content, qr, ticket);
                drawAccessStrip(content, ticket);
                drawImportantNotice(content);
                drawSecurityFooter(content, ticket);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar la boleta PDF.",
                    exception);
        }
    }

    private void drawBackground(PDPageContentStream content) throws IOException {
        fillRect(content, Color.WHITE, 0, 0,
                PDRectangle.A4.getWidth(),
                PDRectangle.A4.getHeight());
        fillRect(content, SURFACE, 0, 0,
                PDRectangle.A4.getWidth(), 70);
    }

    private void drawHeader(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        fillRect(content, NAVY, 0, 682,
                PDRectangle.A4.getWidth(), 160);
        fillRect(content, DARK_GREEN, 0, 682, 168, 160);
        fillRect(content, EVENTIX_LIME, 154, 682, 8, 160);

        writeText(content, "E", 42, 760, 48, true, EVENTIX_GREEN);
        writeText(content, "Event", 80, 760, 29, true, Color.WHITE);
        writeText(content, "ix", 157, 760, 29, true, EVENTIX_LIME);
        writeText(content, "TU EVENTO. TU EXPERIENCIA.",
                42, 732, 9, true, new Color(208, 244, 227));

        writeText(content, "BOLETA DIGITAL", 210, 797, 10, true,
                new Color(196, 235, 217));
        writeText(content, safe(ticket.getEvent().getTitle(), 30),
                210, 755, 29, true, Color.WHITE);
        writeText(content, "ACCESO DIGITAL EVENTIX",
                210, 724, 11, true, EVENTIX_GREEN);
        writeText(content, "Presenta esta boleta desde tu móvil o impresa.",
                210, 700, 10, false, new Color(213, 225, 229));
    }

    private void drawAttendeePanel(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        drawCard(content, 28, 342, 292, 315);
        writeSectionTitle(content, "DATOS DEL ASISTENTE", 48, 628);

        drawInfoRow(content,
                "ASISTENTE",
                safe(ticket.getAttendeeName(), 38),
                48,
                585,
                "01");
        drawDivider(content, 48, 550, 294);

        drawInfoRow(content,
                "TIPO / ZONA",
                safe(ticket.getTicketTypeName(), 32),
                48,
                520,
                "02");
        drawDivider(content, 48, 485, 294);

        drawInfoRow(content,
                "FECHA Y HORA",
                ticket.getEvent().getStartAt().format(DATE_TIME),
                48,
                455,
                "03");
        drawDivider(content, 48, 420, 294);

        drawInfoRow(content,
                "LUGAR",
                safe(ticket.getEvent().getVenue(), 34),
                48,
                390,
                "04");
        writeText(content,
                safe(ticket.getEvent().getAddress(), 52),
                84,
                360,
                9,
                false,
                MUTED);
    }

    private void drawQrPanel(
            PDPageContentStream content,
            PDImageXObject qr,
            DigitalTicket ticket) throws IOException {
        drawCard(content, 335, 342, 232, 315);
        writeText(content, "ESCANEA PARA ACCESO",
                374, 628, 11, true, DARK_GREEN);

        fillRect(content, Color.WHITE, 365, 420, 172, 172);
        strokeRect(content, BORDER, 365, 420, 172, 172, 1);
        content.drawImage(qr, 376, 431, 150, 150);

        writeText(content, "CÓDIGO ÚNICO",
                408, 400, 8, true, MUTED);
        fillRect(content, new Color(230, 246, 239),
                355, 369, 192, 24);
        writeCenteredText(content,
                safe(ticket.getUniqueCode(), 30),
                355,
                376,
                192,
                9,
                true,
                DARK_GREEN);

        writeText(content, "CÓDIGO ANTIFRAUDE",
                398, 350, 8, true, MUTED);
        writeCenteredText(content,
                safe(ticket.getAntiFraudCode(), 30),
                355,
                333,
                192,
                8,
                true,
                TEXT);

        fillRect(content, DARK_GREEN, 398, 302, 107, 24);
        writeCenteredText(content,
                ticket.getStatus().getDisplayName().toUpperCase(),
                398,
                309,
                107,
                9,
                true,
                Color.WHITE);
    }

    private void drawAccessStrip(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        drawCard(content, 28, 242, 539, 80);

        drawMiniBlock(content,
                "ENTRADA",
                safe(ticket.getTicketTypeName(), 18),
                48,
                290);
        drawVerticalDivider(content, 175, 257, 307);
        drawMiniBlock(content,
                "ACCESO",
                "Presenta QR",
                195,
                290);
        drawVerticalDivider(content, 330, 257, 307);
        drawMiniBlock(content,
                "EVENTO",
                safe(ticket.getEvent().getVenue(), 20),
                350,
                290);
    }

    private void drawImportantNotice(PDPageContentStream content)
            throws IOException {
        fillRect(content, DARK_GREEN, 28, 164, 539, 60);
        fillRect(content, WARNING, 42, 178, 30, 30);
        writeText(content, "!", 53, 186, 17, true, NAVY);
        writeText(content, "IMPORTANTE", 86, 199, 10, true, WARNING);
        writeText(content,
                "Presenta este QR en el acceso. No compartas esta boleta.",
                86,
                183,
                9,
                true,
                Color.WHITE);
        writeText(content,
                "Una vez validada, su estado podrá cambiar según las reglas del evento.",
                86,
                169,
                8,
                false,
                new Color(218, 239, 230));
    }

    private void drawSecurityFooter(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        drawCard(content, 28, 78, 539, 68);
        writeText(content, "BOLETA VERIFICADA",
                48, 124, 9, true, DARK_GREEN);
        writeText(content,
                "Firma Ed25519 - " + safe(ticket.getSignatureKeyId(), 22),
                48,
                108,
                8,
                true,
                TEXT);
        writeText(content,
                "Huella SHA-256: " + safe(ticket.getSignedPayloadHash(), 46),
                48,
                94,
                7,
                false,
                MUTED);

        writeText(content, "EVENTIX",
                475, 115, 12, true, NAVY);
        writeText(content, "Seguro. Simple. Digital.",
                433, 96, 8, false, MUTED);
    }

    private void drawInfoRow(
            PDPageContentStream content,
            String label,
            String value,
            float x,
            float y,
            String index) throws IOException {
        fillRect(content, new Color(230, 246, 239), x, y - 6, 24, 24);
        writeCenteredText(content,
                index,
                x,
                y + 1,
                24,
                8,
                true,
                DARK_GREEN);
        writeText(content, label, x + 36, y + 8, 8, true, DARK_GREEN);
        writeText(content, value, x + 36, y - 12, 13, true, TEXT);
    }

    private void drawMiniBlock(
            PDPageContentStream content,
            String label,
            String value,
            float x,
            float y) throws IOException {
        writeText(content, label, x, y, 8, true, DARK_GREEN);
        writeText(content, value, x, y - 20, 11, true, TEXT);
    }

    private void writeSectionTitle(
            PDPageContentStream content,
            String value,
            float x,
            float y) throws IOException {
        fillRect(content, EVENTIX_GREEN, x, y - 2, 18, 3);
        writeText(content, value, x + 28, y - 5, 10, true, NAVY);
    }

    private void drawCard(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height) throws IOException {
        fillRect(content, Color.WHITE, x, y, width, height);
        strokeRect(content, BORDER, x, y, width, height, 0.8f);
    }

    private void drawDivider(
            PDPageContentStream content,
            float x,
            float y,
            float x2) throws IOException {
        content.setStrokingColor(BORDER);
        content.setLineWidth(0.8f);
        content.moveTo(x, y);
        content.lineTo(x2, y);
        content.stroke();
    }

    private void drawVerticalDivider(
            PDPageContentStream content,
            float x,
            float y,
            float y2) throws IOException {
        content.setStrokingColor(BORDER);
        content.setLineWidth(0.8f);
        content.moveTo(x, y);
        content.lineTo(x, y2);
        content.stroke();
    }

    private void fillRect(
            PDPageContentStream content,
            Color color,
            float x,
            float y,
            float width,
            float height) throws IOException {
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void strokeRect(
            PDPageContentStream content,
            Color color,
            float x,
            float y,
            float width,
            float height,
            float lineWidth) throws IOException {
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void writeCenteredText(
            PDPageContentStream content,
            String value,
            float x,
            float y,
            float width,
            float size,
            boolean bold,
            Color color) throws IOException {
        PDType1Font font = font(bold);
        float textWidth = font.getStringWidth(value) / 1000 * size;
        float textX = x + Math.max(0, (width - textWidth) / 2);
        writeText(content, value, textX, y, size, bold, color);
    }

    private void writeText(
            PDPageContentStream content,
            String value,
            float x,
            float y,
            float size,
            boolean bold,
            Color color) throws IOException {
        content.beginText();
        content.setFont(font(bold), size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    private PDType1Font font(boolean bold) {
        return new PDType1Font(bold
                ? Standard14Fonts.FontName.HELVETICA_BOLD
                : Standard14Fonts.FontName.HELVETICA);
    }

    private String safe(String value, int maximumLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength - 3) + "...";
    }
}
