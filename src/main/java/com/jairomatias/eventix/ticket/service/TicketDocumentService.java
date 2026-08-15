package com.jairomatias.eventix.ticket.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import com.jairomatias.eventix.shared.pdf.PdfBranding;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

@Service
public class TicketDocumentService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Color NAVY = new Color(3, 27, 38);
    private static final Color NAVY_2 = new Color(4, 50, 55);
    private static final Color GREEN = new Color(14, 149, 92);
    private static final Color LIME = new Color(139, 214, 44);
    private static final Color TEAL = new Color(4, 121, 98);
    private static final Color TEXT = new Color(11, 27, 39);
    private static final Color MUTED = new Color(82, 101, 108);
    private static final Color BORDER = new Color(220, 229, 227);
    private static final Color SOFT = new Color(246, 249, 248);
    private static final Color SOFT_GREEN = new Color(237, 247, 242);
    private static final Color WARNING = new Color(243, 180, 31);

    private final TicketCryptographyService cryptographyService;
    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public TicketDocumentService(TicketCryptographyService cryptographyService) {
        this.cryptographyService = cryptographyService;
    }

    public byte[] createQrPng(DigitalTicket ticket) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    cryptographyService.createQrPayload(ticket),
                    BarcodeFormat.QR_CODE,
                    520,
                    520);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("No se pudo generar el código QR.", exception);
        }
    }

    public byte[] createPdf(DigitalTicket ticket) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject qr = PDImageXObject.createFromByteArray(
                    document, createQrPng(ticket), "eventix-ticket-qr");

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawBackground(content);
                drawHero(document, content, ticket);
                drawDetailsPanel(content, ticket);
                drawQrPanel(content, qr, ticket);
                drawAccessStrip(content, ticket);
                drawWarningStrip(content);
                drawVerificationFooter(content, ticket);
                drawBottomBrandBar(content);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar la boleta PDF.", exception);
        }
    }

    private void drawBackground(PDPageContentStream content) throws IOException {
        fillRect(content, 0, 0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight(), SOFT);
        roundRect(content, 22, 18, 551, 806, 12, Color.WHITE, BORDER);
    }

    private void drawHero(PDDocument document, PDPageContentStream content,
            DigitalTicket ticket) throws IOException {
        fillRect(content, 22, 651, 551, 173, NAVY);
        fillRect(content, 22, 651, 551, 8, GREEN);
        fillRect(content, 312, 651, 261, 173, NAVY_2);
        fillRect(content, 306, 651, 7, 173, LIME);
        drawHeroAccent(content);
        PdfBranding.drawOfficialLogo(document, content, 48, 690, 198, 104);

        writeText(content, "BOLETA DIGITAL", 386, 788, 13, true, Color.WHITE);
        drawShortLine(content, 356, 783, 22, GREEN);
        drawShortLine(content, 501, 783, 22, GREEN);

        String title = value(ticket.getEvent().getTitle()).toUpperCase();
        float titleSize = fittedFontSize(title, bold, 205, 27, 17);
        writeWrappedText(content, title, 353, 736, 205, titleSize, true,
                Color.WHITE, titleSize + 3, 3);
        writeText(content, "TU EVENTO. TU EXPERIENCIA.", 50, 675, 10, true, LIME);
    }

    private void drawHeroAccent(PDPageContentStream content) throws IOException {
        content.setStrokingColor(new Color(9, 99, 78));
        content.setLineWidth(2f);
        for (int i = 0; i < 5; i++) {
            float y = 674 + (i * 18);
            content.moveTo(330, y);
            content.lineTo(548, y + 22);
            content.stroke();
        }
    }

    private void drawDetailsPanel(PDPageContentStream content, DigitalTicket ticket)
            throws IOException {
        float x = 48;
        float y = 292;
        float width = 250;
        roundRect(content, x, y, width, 335, 12, Color.WHITE, Color.WHITE);

        drawInfoRow(content, "A", "ASISTENTE", value(ticket.getAttendeeName()),
                x + 18, y + 270, 180, 2);
        drawSeparator(content, x + 18, y + 236, width - 36);
        drawInfoRow(content, "T", "TIPO / ZONA", value(ticket.getTicketTypeName()),
                x + 18, y + 190, 180, 2);
        drawSeparator(content, x + 18, y + 156, width - 36);
        drawInfoRow(content, "F", "FECHA Y HORA",
                ticket.getEvent().getStartAt().format(DATE_TIME),
                x + 18, y + 110, 180, 1);
        drawSeparator(content, x + 18, y + 76, width - 36);
        drawInfoRow(content, "L", "LUGAR", value(ticket.getEvent().getVenue()),
                x + 18, y + 30, 180, 2);
        writeWrappedText(content, value(ticket.getEvent().getAddress()),
                x + 67, y + 12, 180, 7.2f, false, MUTED, 8.5f, 2);
    }

    private void drawInfoRow(PDPageContentStream content, String icon, String label,
            String text, float x, float y, float maxWidth, int maxLines) throws IOException {
        drawCircle(content, x + 18, y + 18, 17, TEAL);
        writeText(content, icon, x + 13, y + 12, 11, true, Color.WHITE);
        writeText(content, label, x + 53, y + 27, 9, true, TEAL);
        float size = fittedFontSize(text, bold, maxWidth, 15, 10);
        writeWrappedText(content, text, x + 53, y + 9, maxWidth, size, true,
                TEXT, size + 2, maxLines);
    }

    private void drawQrPanel(PDPageContentStream content, PDImageXObject qr,
            DigitalTicket ticket) throws IOException {
        float x = 310;
        float y = 292;
        float width = 235;
        roundRect(content, x, y, width, 335, 16, SOFT_GREEN, BORDER);
        writeText(content, "ESCANEA PARA ACCESO", x + 55, y + 307, 10, true, TEAL);

        // La zona blanca y el tamaño del QR se mantienen fijos para preservar lecturabilidad.
        roundRect(content, x + 39, y + 139, 157, 157, 9, Color.WHITE, BORDER);
        content.drawImage(qr, x + 48, y + 148, 139, 139);

        labelCentered(content, "CÓDIGO ÚNICO", x, width, y + 118, TEAL);
        roundRect(content, x + 20, y + 82, width - 40, 27, 13,
                new Color(224, 239, 232), new Color(224, 239, 232));
        labelCenteredAdaptive(content, value(ticket.getUniqueCode()), x + 20,
                width - 40, y + 91, TEAL, 8.5f, 6.5f);

        labelCentered(content, "CÓDIGO ANTIFRAUDE", x, width, y + 61, TEAL);
        roundRect(content, x + 20, y + 26, width - 40, 27, 13,
                new Color(239, 241, 241), new Color(239, 241, 241));
        labelCenteredAdaptive(content, value(ticket.getAntiFraudCode()), x + 20,
                width - 40, y + 35, TEXT, 8.5f, 6.5f);

        drawStatusBadge(content, value(ticket.getStatus().getDisplayName()).toUpperCase(),
                x + 71, y - 7);
    }

    private void drawStatusBadge(PDPageContentStream content, String status, float x, float y)
            throws IOException {
        roundRect(content, x, y, 94, 25, 12, TEAL, TEAL);
        drawCircle(content, x + 15, y + 12.5f, 6, Color.WHITE);
        writeText(content, "V", x + 12, y + 9, 7, true, TEAL);
        float size = fittedFontSize(status, bold, 58, 10, 7);
        writeText(content, status, x + 30, y + 8, size, true, Color.WHITE);
    }

    private void drawAccessStrip(PDPageContentStream content, DigitalTicket ticket)
            throws IOException {
        float x = 48;
        float y = 226;
        float height = 52;
        roundRect(content, x, y, 497, height, 10, Color.WHITE, BORDER);
        drawAccessItem(content, "ENTRADA", value(ticket.getTicketTypeName()), x + 18, y + 15, 105);
        drawVerticalDivider(content, x + 124, y + 10, height - 20);
        drawAccessItem(content, "ACCESO", "QR oficial", x + 139, y + 15, 94);
        drawVerticalDivider(content, x + 245, y + 10, height - 20);
        drawAccessItem(content, "INICIO", ticket.getEvent().getStartAt().format(TIME),
                x + 260, y + 15, 84);
        drawVerticalDivider(content, x + 356, y + 10, height - 20);
        drawAccessItem(content, "ESTADO", value(ticket.getStatus().getDisplayName()),
                x + 371, y + 15, 105);
    }

    private void drawAccessItem(PDPageContentStream content, String label, String text,
            float x, float y, float width) throws IOException {
        writeText(content, label, x, y + 20, 8, true, TEAL);
        float size = fittedFontSize(text, bold, width - 4, 10, 6.5f);
        writeText(content, text, x, y + 5, size, true, TEXT);
    }

    private void drawWarningStrip(PDPageContentStream content) throws IOException {
        float x = 48;
        float y = 156;
        roundRect(content, x, y, 497, 54, 9, NAVY_2, NAVY_2);
        drawCircle(content, x + 28, y + 27, 15, WARNING);
        writeText(content, "!", x + 25, y + 20, 15, true, NAVY);
        writeText(content, "IMPORTANTE:", x + 53, y + 33, 9, true, WARNING);
        writeText(content, "Presenta este QR en el acceso. No compartas esta boleta.",
                x + 128, y + 33, 8.5f, true, Color.WHITE);
        writeText(content, "Una vez escaneada, la boleta quedará invalidada.",
                x + 128, y + 16, 8, false, Color.WHITE);
    }

    private void drawVerificationFooter(PDPageContentStream content, DigitalTicket ticket)
            throws IOException {
        float x = 48;
        float y = 74;
        float height = 67;
        roundRect(content, x, y, 497, height, 9, SOFT, BORDER);
        drawCircle(content, x + 29, y + 35, 19, TEAL);
        writeText(content, "OK", x + 20, y + 31, 9, true, Color.WHITE);
        writeText(content, "BOLETA VERIFICADA", x + 57, y + 46, 8, true, TEAL);
        writeText(content, "Firma digital Eventix", x + 57, y + 29, 8.5f, true, TEXT);
        writeText(content, "Protegida con validación criptográfica y código antifraude.",
                x + 57, y + 14, 7.5f, false, MUTED);
        drawVerticalDivider(content, x + 350, y + 12, height - 24);
        writeText(content, "GENERADA PARA", x + 365, y + 46, 7, true, TEAL);
        String attendee = value(ticket.getAttendeeName());
        float size = fittedFontSize(attendee, bold, 126, 8.5f, 6.5f);
        writeWrappedText(content, attendee, x + 365, y + 32, 126, size, true,
                TEXT, size + 1.5f, 2);
        writeText(content, ticket.getEvent().getStartAt().format(DATE_TIME),
                x + 365, y + 10, 7.5f, false, MUTED);
    }

    private void drawBottomBrandBar(PDPageContentStream content) throws IOException {
        fillRect(content, 22, 18, 551, 32, NAVY);
        fillRect(content, 22, 18, 551, 4, GREEN);
        writeText(content, "EVENTIX  -  PLATAFORMA OFICIAL DE GESTIÓN DE EVENTOS",
                152, 30, 7.5f, true, Color.WHITE);
    }

    private void writeWrappedText(PDPageContentStream content, String text, float x,
            float firstBaseline, float maxWidth, float size, boolean boldText,
            Color color, float leading, int maxLines) throws IOException {
        List<String> lines = wrap(text, boldText ? bold : regular, size, maxWidth);
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            writeText(content, lines.get(i), x, firstBaseline - (i * leading),
                    size, boldText, color);
        }
        if (lines.size() > maxLines) {
            // Si una cadena extrema excede el número de líneas, se reduce hasta que quepa,
            // nunca se reemplaza información por puntos suspensivos.
            float reduced = Math.max(5.5f, size - 1.5f);
            if (reduced < size) {
                writeWrappedText(content, text, x, firstBaseline, maxWidth,
                        reduced, boldText, color, reduced + 1.5f, maxLines);
            }
        }
    }

    private List<String> wrap(String text, PDType1Font font, float size, float maxWidth)
            throws IOException {
        String normalized = value(text).trim();
        List<String> lines = new ArrayList<>();
        if (normalized.isEmpty()) {
            lines.add("");
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textWidth(candidate, font, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (textWidth(word, font, size) <= maxWidth) {
                    line.append(word);
                } else {
                    splitLongToken(word, font, size, maxWidth, lines, line);
                }
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private void splitLongToken(String token, PDType1Font font, float size, float maxWidth,
            List<String> lines, StringBuilder remainder) throws IOException {
        StringBuilder chunk = new StringBuilder();
        for (char character : token.toCharArray()) {
            String candidate = chunk.toString() + character;
            if (!chunk.isEmpty() && textWidth(candidate, font, size) > maxWidth) {
                lines.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(character);
        }
        remainder.append(chunk);
    }

    private float fittedFontSize(String text, PDType1Font font, float maxWidth,
            float preferred, float minimum) throws IOException {
        float size = preferred;
        while (size > minimum && textWidth(value(text), font, size) > maxWidth * 2f) {
            size -= 0.5f;
        }
        return size;
    }

    private float textWidth(String text, PDType1Font font, float size) throws IOException {
        return font.getStringWidth(pdfSafe(text)) / 1000f * size;
    }

    private void labelCentered(PDPageContentStream content, String text, float x,
            float width, float y, Color color) throws IOException {
        labelCenteredAdaptive(content, text, x, width, y, color, 8.5f, 7f);
    }

    private void labelCenteredAdaptive(PDPageContentStream content, String text, float x,
            float width, float y, Color color, float preferred, float minimum) throws IOException {
        float size = preferred;
        while (size > minimum && textWidth(text, bold, size) > width - 10) {
            size -= 0.25f;
        }
        float actualWidth = textWidth(text, bold, size);
        writeText(content, text, x + Math.max(5, (width - actualWidth) / 2f), y,
                size, true, color);
    }

    private void drawSeparator(PDPageContentStream content, float x, float y, float width)
            throws IOException {
        content.setStrokingColor(BORDER);
        content.setLineDashPattern(new float[]{4, 4}, 0);
        content.moveTo(x, y);
        content.lineTo(x + width, y);
        content.stroke();
        content.setLineDashPattern(new float[]{}, 0);
    }

    private void drawVerticalDivider(PDPageContentStream content, float x, float y, float height)
            throws IOException {
        content.setStrokingColor(BORDER);
        content.moveTo(x, y);
        content.lineTo(x, y + height);
        content.stroke();
    }

    private void drawShortLine(PDPageContentStream content, float x, float y, float width,
            Color color) throws IOException {
        content.setStrokingColor(color);
        content.setLineWidth(1.5f);
        content.moveTo(x, y);
        content.lineTo(x + width, y);
        content.stroke();
    }

    private void fillRect(PDPageContentStream content, float x, float y, float width,
            float height, Color color) throws IOException {
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void roundRect(PDPageContentStream content, float x, float y, float width,
            float height, float radius, Color fill, Color stroke) throws IOException {
        float c = radius * 0.552284749831f;
        float right = x + width;
        float top = y + height;
        content.setNonStrokingColor(fill);
        content.setStrokingColor(stroke);
        content.moveTo(x + radius, y);
        content.lineTo(right - radius, y);
        content.curveTo(right - radius + c, y, right, y + radius - c, right, y + radius);
        content.lineTo(right, top - radius);
        content.curveTo(right, top - radius + c, right - radius + c, top, right - radius, top);
        content.lineTo(x + radius, top);
        content.curveTo(x + radius - c, top, x, top - radius + c, x, top - radius);
        content.lineTo(x, y + radius);
        content.curveTo(x, y + radius - c, x + radius - c, y, x + radius, y);
        content.closePath();
        content.fillAndStroke();
    }

    private void drawCircle(PDPageContentStream content, float centerX, float centerY,
            float radius, Color color) throws IOException {
        float c = radius * 0.552284749831f;
        content.setNonStrokingColor(color);
        content.moveTo(centerX + radius, centerY);
        content.curveTo(centerX + radius, centerY + c, centerX + c, centerY + radius,
                centerX, centerY + radius);
        content.curveTo(centerX - c, centerY + radius, centerX - radius, centerY + c,
                centerX - radius, centerY);
        content.curveTo(centerX - radius, centerY - c, centerX - c, centerY - radius,
                centerX, centerY - radius);
        content.curveTo(centerX + c, centerY - radius, centerX + radius, centerY - c,
                centerX + radius, centerY);
        content.closePath();
        content.fill();
    }

    private void writeText(PDPageContentStream content, String text, float x, float y,
            float size, boolean boldText, Color color) throws IOException {
        content.beginText();
        content.setFont(boldText ? bold : regular, size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(text));
        content.endText();
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private String pdfSafe(String text) {
        return value(text).replace("·", "-")
                .replaceAll("[^\\x20-\\x7EÀ-ÿ]", "-");
    }
}
