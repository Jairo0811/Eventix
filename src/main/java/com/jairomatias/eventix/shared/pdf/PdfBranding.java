package com.jairomatias.eventix.shared.pdf;

import java.awt.Color;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class PdfBranding {

    private static final Color NAVY = new Color(6, 25, 35);
    private static final Color TEAL = new Color(20, 184, 166);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color LIME = new Color(163, 230, 53);

    private PdfBranding() {
    }

    public static void drawOfficialLogo(
            PDDocument document,
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height) throws IOException {
        float scale = Math.min(width / 190f, height / 86f);
        float markWidth = 52f * scale;
        float markHeight = 42f * scale;
        float markX = x;
        float markY = y + (height - markHeight) / 2f + (12f * scale);

        drawMotionLine(content, markX, markY + (29f * scale), 19f * scale, 4f * scale);
        drawMotionLine(content, markX + (3f * scale), markY + (20f * scale), 25f * scale, 4f * scale);
        drawMotionLine(content, markX, markY + (11f * scale), 18f * scale, 4f * scale);

        content.setNonStrokingColor(GREEN);
        content.moveTo(markX + (22f * scale), markY + (4f * scale));
        content.lineTo(markX + (52f * scale), markY + (4f * scale));
        content.lineTo(markX + (60f * scale), markY + (13f * scale));
        content.lineTo(markX + (52f * scale), markY + (38f * scale));
        content.lineTo(markX + (23f * scale), markY + (38f * scale));
        content.lineTo(markX + (15f * scale), markY + (29f * scale));
        content.closePath();
        content.fill();

        content.setNonStrokingColor(LIME);
        content.addRect(
                markX + (28f * scale),
                markY + (29f * scale),
                28f * scale,
                9f * scale);
        content.fill();

        content.setNonStrokingColor(NAVY);
        float eX = markX + (28f * scale);
        content.addRect(eX, markY + (10f * scale), 5f * scale, 21f * scale);
        content.addRect(eX, markY + (26f * scale), 18f * scale, 5f * scale);
        content.addRect(eX, markY + (18f * scale), 14f * scale, 5f * scale);
        content.addRect(eX, markY + (10f * scale), 18f * scale, 5f * scale);
        content.fill();

        content.setNonStrokingColor(TEAL);
        content.moveTo(markX + (49f * scale), markY + (8f * scale));
        content.lineTo(markX + (69f * scale), markY + (8f * scale));
        content.lineTo(markX + (69f * scale), markY + (3f * scale));
        content.lineTo(markX + (80f * scale), markY + (15f * scale));
        content.lineTo(markX + (69f * scale), markY + (27f * scale));
        content.lineTo(markX + (69f * scale), markY + (22f * scale));
        content.lineTo(markX + (49f * scale), markY + (22f * scale));
        content.closePath();
        content.fill();

        float wordX = markX + (87f * scale);
        float wordY = y + (height / 2f) + (2f * scale);
        write(content, "Event", wordX, wordY, 22f * scale, NAVY);
        write(content, "ix", wordX + (59f * scale), wordY, 22f * scale, GREEN);
        write(content, "TU EVENTO. TU EXPERIENCIA.",
                wordX, wordY - (12f * scale), 5.1f * scale, TEAL);
    }

    private static void drawMotionLine(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height) throws IOException {
        content.setNonStrokingColor(TEAL);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private static void write(
            PDPageContentStream content,
            String text,
            float x,
            float y,
            float size,
            Color color) throws IOException {
        content.beginText();
        content.setFont(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
}
