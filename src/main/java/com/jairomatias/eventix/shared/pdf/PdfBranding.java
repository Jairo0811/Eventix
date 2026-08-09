package com.jairomatias.eventix.shared.pdf;

import java.awt.Color;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Reusable Eventix branding primitives for generated PDF documents.
 *
 * <p>The PDF logo is rendered with native PDFBox vector/text operations instead
 * of loading an external raster asset. This keeps ticket/report generation
 * deterministic and avoids failures caused by malformed or missing image
 * resources inside the executable JAR.</p>
 */
public final class PdfBranding {

    private static final Color GREEN = new Color(120, 221, 0);
    private static final Color TEAL = new Color(0, 201, 207);
    private static final Color DARK = new Color(4, 52, 64);
    private static final Color WHITE = new Color(248, 250, 252);

    private PdfBranding() {
    }

    public static void drawOfficialLogo(
            PDDocument document,
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height) throws IOException {
        float scale = Math.min(width / 210f, height / 82f);
        float logoWidth = 210f * scale;
        float logoHeight = 82f * scale;
        float originX = x + ((width - logoWidth) / 2f);
        float originY = y + ((height - logoHeight) / 2f);

        drawMark(content, originX, originY + (30f * scale), scale);
        drawWordmark(content, originX, originY, scale);
    }

    private static void drawMark(
            PDPageContentStream content,
            float x,
            float y,
            float scale) throws IOException {
        content.saveGraphicsState();

        content.setNonStrokingColor(TEAL);
        content.addRect(x, y + (19f * scale), 23f * scale, 5f * scale);
        content.fill();
        content.addRect(x + (8f * scale), y + (30f * scale), 30f * scale, 5f * scale);
        content.fill();
        content.addRect(x + (10f * scale), y + (8f * scale), 28f * scale, 5f * scale);
        content.fill();

        content.setNonStrokingColor(GREEN);
        content.moveTo(x + (38f * scale), y + (7f * scale));
        content.lineTo(x + (103f * scale), y + (27f * scale));
        content.lineTo(x + (98f * scale), y + (46f * scale));
        content.lineTo(x + (33f * scale), y + (26f * scale));
        content.closePath();
        content.fill();

        content.setNonStrokingColor(DARK);
        content.setFont(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                24f * scale);
        content.beginText();
        content.newLineAtOffset(x + (54f * scale), y + (17f * scale));
        content.showText("E");
        content.endText();

        content.setNonStrokingColor(GREEN);
        content.moveTo(x + (96f * scale), y + (19f * scale));
        content.lineTo(x + (120f * scale), y + (25f * scale));
        content.lineTo(x + (105f * scale), y + (40f * scale));
        content.closePath();
        content.fill();

        content.restoreGraphicsState();
    }

    private static void drawWordmark(
            PDPageContentStream content,
            float x,
            float y,
            float scale) throws IOException {
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        content.setNonStrokingColor(WHITE);
        content.setFont(bold, 25f * scale);
        content.beginText();
        content.newLineAtOffset(x, y + (7f * scale));
        content.showText("Event");
        content.endText();

        float eventWidth = bold.getStringWidth("Event") / 1000f * 25f * scale;
        content.setNonStrokingColor(TEAL);
        content.beginText();
        content.newLineAtOffset(x + eventWidth, y + (7f * scale));
        content.showText("ix");
        content.endText();

        content.setNonStrokingColor(GREEN);
        content.setFont(bold, 5.4f * scale);
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText("TRANSFORMA TUS EVENTOS. CONECTA EXPERIENCIAS.");
        content.endText();
    }
}
