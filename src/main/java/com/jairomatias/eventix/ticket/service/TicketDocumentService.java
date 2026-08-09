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
            DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private static final Color NAVY = new Color(4, 32, 43);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color TEAL = new Color(5, 150, 105);
    private static final Color TEXT = new Color(20, 32, 37);
    private static final Color MUTED = new Color(93, 108, 113);
    private static final Color BORDER = new Color(220, 228, 226);
    private static final Color SOFT = new Color(246, 249, 248);
    private static final Color LIGHT_GREEN = new Color(232, 247, 238);

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
                drawBrandHeader(content);
                drawEventHero(content, ticket);
                drawDetailsCard(content, ticket);
                drawQrCard(content, qr, ticket);
                drawTicketDivider(content);
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
        fillRect(content, 0, 0, PDRectangle.A4.getWidth(),
                PDRectangle.A4.getHeight(), SOFT);
        fillRect(content, 28, 28, 539, 786, Color.WHITE);
    }

    private void drawBrandHeader(PDPageContentStream content) throws IOException {
        fillRect(content, 28, 704, 539, 110, NAVY);
        fillRect(content, 28, 704, 8, 110, GREEN);
        fillRect(content, 50, 752, 34, 34, GREEN);

        writeText(content, "E", 60, 760, 20, true, Color.WHITE);
        writeText(content, "Eventix", 96, 766, 24, true, Color.WHITE);
        writeText(content, "BOLETA DIGITAL", 96, 746, 9, true,
                new Color(185, 232, 211));
        writeText(content, "ACCESO OFICIAL", 432, 767, 9, true,
                new Color(185, 232, 211));
        writeText(content, "Seguro · rápido · verificable",
                382, 747, 8, false, Color.WHITE);
    }

    private void drawEventHero(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        writeText(content, "TU ENTRADA PARA", 50, 673, 9, true, TEAL);
        writeText(content, safe(ticket.getEvent().getTitle(), 42),
                50, 642, 24, true, TEXT);
        writeText(content,
                ticket.getEvent().getStartAt().format(DATE_TIME),
                50, 617, 11, true, MUTED);
        writeText(content, safe(ticket.getEvent().getVenue(), 58),
                50, 598, 11, false, MUTED);
        writeText(content, safe(ticket.getEvent().getAddress(), 76),
                50, 581, 9, false, MUTED);
    }

    private void drawDetailsCard(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        strokeAndFillRect(content, 50, 388, 266, 163, Color.WHITE, BORDER);

        label(content, "ASISTENTE", 68, 526);
        writeText(content, safe(ticket.getAttendeeName(), 34),
                68, 506, 14, true, TEXT);

        label(content, "TIPO / ZONA", 68, 476);
        writeText(content, safe(ticket.getTicketTypeName(), 32),
                68, 456, 13, true, TEXT);

        label(content, "CÓDIGO ÚNICO", 68, 426);
        writeText(content, safe(ticket.getUniqueCode(), 34),
                68, 406, 11, true, TEAL);
    }

    private void drawQrCard(
            PDPageContentStream content,
            PDImageXObject qr,
            DigitalTicket ticket) throws IOException {
        strokeAndFillRect(content, 330, 388, 215, 163, LIGHT_GREEN, BORDER);
        content.drawImage(qr, 356, 411, 112, 112);

        fillRect(content, 476, 482, 52, 23, GREEN);
        writeText(content,
                safe(ticket.getStatus().getDisplayName().toUpperCase(), 10),
                484, 489, 8, true, Color.WHITE);
        writeText(content, "ESCANEA EN EL ACCESO",
                355, 396, 8, true, TEAL);
    }

    private void drawTicketDivider(PDPageContentStream content) throws IOException {
        content.setStrokingColor(BORDER);
        content.setLineDashPattern(new float[]{5, 5}, 0);
        content.moveTo(50, 356);
        content.lineTo(545, 356);
        content.stroke();
        content.setLineDashPattern(new float[]{}, 0);

        fillRect(content, 28, 344, 12, 24, SOFT);
        fillRect(content, 555, 344, 12, 24, SOFT);
    }

    private void drawSecurityFooter(
            PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        writeText(content, "BOLETA AUTÉNTICA", 50, 321, 10, true, GREEN);
        writeText(content,
                "Validada y firmada digitalmente por Eventix.",
                50, 301, 10, false, TEXT);

        label(content, "CÓDIGO ANTIFRAUDE", 50, 270);
        writeText(content, safe(ticket.getAntiFraudCode(), 42),
                50, 250, 10, true, TEXT);

        strokeAndFillRect(content, 50, 183, 495, 43, SOFT, BORDER);
        writeText(content,
                "Presenta el QR original en el acceso. No compartas capturas ni copias.",
                66, 207, 9, true, NAVY);
        writeText(content,
                "Conserva esta boleta hasta finalizar el evento.",
                66, 191, 8, false, MUTED);

        writeText(content, "Eventix · Ticketing & Access",
                50, 145, 8, true, MUTED);
        writeText(content, "Documento generado automáticamente",
                378, 145, 8, false, MUTED);
    }

    private void label(
            PDPageContentStream content,
            String value,
            float x,
            float y) throws IOException {
        writeText(content, value, x, y, 8, true, MUTED);
    }

    private void fillRect(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            Color color) throws IOException {
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void strokeAndFillRect(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            Color fill,
            Color stroke) throws IOException {
        content.setNonStrokingColor(fill);
        content.setStrokingColor(stroke);
        content.addRect(x, y, width, height);
        content.fillAndStroke();
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
        content.setFont(
                new PDType1Font(bold
                        ? Standard14Fonts.FontName.HELVETICA_BOLD
                        : Standard14Fonts.FontName.HELVETICA),
                size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(value));
        content.endText();
    }

    private String safe(String value, int maximumLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength - 3) + "...";
    }

    private String pdfSafe(String value) {
        return value == null
                ? ""
                : value.replace("·", "-")
                        .replaceAll("[^\\x20-\\x7EÀ-ÿ]", "-");
    }
}
