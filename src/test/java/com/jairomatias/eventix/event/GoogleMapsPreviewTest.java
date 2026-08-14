package com.jairomatias.eventix.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GoogleMapsPreviewTest {

    @Test
    void previewUsesOfficialEmbedWhenApiKeyExistsAndFallbackOtherwise() throws IOException {
        String script = resource("static/js/app.js");

        assertThat(script)
                .contains("https://www.google.com/maps/embed/v1/place?")
                .contains("https://www.google.com/maps?")
                .contains("output: \"embed\"")
                .contains("safeMapsUrl || buildSearchUrl(query)")
                .contains("El enlace compartido se conserva; la vista previa");
    }

    @Test
    void shortGoogleMapsLinksFallBackToVenueAndAddress() throws IOException {
        String script = resource("static/js/app.js");

        assertThat(script)
                .contains("maps.app.goo.gl")
                .contains("const query = parsedQuery || fallbackQuery()")
                .contains("venueInput?.value.trim()")
                .contains("addressInput?.value.trim()");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
