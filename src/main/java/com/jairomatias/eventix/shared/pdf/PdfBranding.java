package com.jairomatias.eventix.shared.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public final class PdfBranding {

    private static final String LOGO_RESOURCE = "/branding/eventix-logo.base64";

    private PdfBranding() {
    }

    public static void drawOfficialLogo(
            PDDocument document,
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height) throws IOException {
        PDImageXObject logo = PDImageXObject.createFromByteArray(
                document,
                loadLogoBytes(),
                "eventix-official-logo");
        content.drawImage(logo, x, y, width, height);
    }

    private static byte[] loadLogoBytes() throws IOException {
        try (InputStream input = PdfBranding.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (input == null) {
                throw new IOException(
                        "No se encontró el recurso de branding " + LOGO_RESOURCE);
            }
            String encoded = new String(
                    input.readAllBytes(),
                    StandardCharsets.US_ASCII).replaceAll("\\s+", "");
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "El recurso del logo Eventix no contiene Base64 válido.",
                    exception);
        }
    }
}
