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
                content.setNonStrokingColor(new Color(15, 122, 82));
                content.addRect(0, 742, PDRectangle.A4.getWidth(), 100);
                content.fill();

                writeText(content, "EVENTIX", 44, 790, 26, true, 255, 255, 255);
                writeText(content, "BOLETA DIGITAL", 44, 765, 12, false, 229, 255, 245);

                writeText(content, safe(ticket.getEvent().getTitle(), 48), 44, 705, 22, true, 22, 33, 29);
                writeText(content, "Asistente", 44, 665, 10, true, 90, 103, 98);
                writeText(content, safe(ticket.getAttendeeName(), 54), 44, 646, 15, false, 22, 33, 29);
                writeText(content, "Tipo / zona", 44, 612, 10, true, 90, 103, 98);
                writeText(content, safe(ticket.getTicketTypeName(), 46), 44, 593, 15, false, 22, 33, 29);
                writeText(content, "Fecha y hora", 44, 559, 10, true, 90, 103, 98);
                writeText(content, ticket.getEvent().getStartAt().format(DATE_TIME), 44, 540, 15, false, 22, 33, 29);
                writeText(content, "Lugar", 44, 506, 10, true, 90, 103, 98);
                writeText(content, safe(ticket.getEvent().getVenue(), 50), 44, 487, 15, false, 22, 33, 29);
                writeText(content, safe(ticket.getEvent().getAddress(), 72), 44, 468, 11, false, 80, 92, 87);

                content.drawImage(qr, 312, 435, 235, 235);

                writeText(content, "Código único", 44, 410, 10, true, 90, 103, 98);
                writeText(content, ticket.getUniqueCode(), 44, 390, 14, true, 15, 122, 82);
                writeText(content, "Código antifraude", 44, 356, 10, true, 90, 103, 98);
                writeText(content, ticket.getAntiFraudCode(), 44, 337, 12, false, 22, 33, 29);
                writeText(content, "Estado", 44, 303, 10, true, 90, 103, 98);
                writeText(content, ticket.getStatus().getDisplayName(), 44, 284, 14, true, 22, 33, 29);

                content.setStrokingColor(new Color(210, 219, 215));
                content.moveTo(44, 245);
                content.lineTo(550, 245);
                content.stroke();
                writeText(content, "Firma Ed25519 · " + ticket.getSignatureKeyId(), 44, 220, 9, true, 90, 103, 98);
                writeText(content, "Huella: " + ticket.getSignedPayloadHash(), 44, 204, 8, false, 90, 103, 98);
                writeText(content, "Presenta este QR en el acceso. No compartas esta boleta.", 44, 160, 11, true, 15, 122, 82);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar la boleta PDF.",
                    exception);
        }
    }

    private void writeText(
            PDPageContentStream content,
            String value,
            float x,
            float y,
            float size,
            boolean bold,
            int red,
            int green,
            int blue) throws IOException {
        content.beginText();
        content.setFont(
                new PDType1Font(bold
                        ? Standard14Fonts.FontName.HELVETICA_BOLD
                        : Standard14Fonts.FontName.HELVETICA),
                size);
        content.setNonStrokingColor(new Color(red, green, blue));
        content.newLineAtOffset(x, y);
        content.showText(value);
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
}
