package com.jairomatias.eventix.home;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PublicStorefrontResponsiveContractTest {

    @Test
    void catalogPaginationAdaptsToNarrowScreens() throws IOException {
        String catalog = resource("templates/public/events.html");
        String css = resource("static/css/public.css");

        assertThat(catalog)
                .contains("public-pagination-prev")
                .contains("public-pagination-next");
        assertThat(css)
                .contains(".public-pagination { align-items: center; display: flex; flex-wrap: wrap;")
                .contains(".public-pagination > span { white-space: nowrap; }")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))")
                .contains(".public-pagination-next { grid-column: 2; }");
    }

    @Test
    void longEventAndTicketLabelsCanWrapWithoutShrinkingPrices() throws IOException {
        String css = resource("static/css/public.css");

        assertThat(css)
                .contains(".public-card-link { color: inherit; display: block; height: 100%; overflow-wrap: anywhere;")
                .contains(".ticket-row > div { min-width: 0; overflow-wrap: anywhere; }")
                .contains(".ticket-price { color: #15803d; flex-shrink: 0; white-space: nowrap; }")
                .contains(".home-event-card-footer { flex-wrap: wrap; gap: .5rem; }");
    }

    @Test
    void shareActionMatchesDeviceCapabilitiesAndAnnouncesCopyFeedback() throws IOException {
        String detail = resource("templates/public/event-detail.html");
        String script = resource("static/js/storefront.js");

        assertThat(detail).contains("data-share-label aria-live=\"polite\" aria-atomic=\"true\"");
        assertThat(script)
                .contains("if (navigator.share && label)")
                .contains("original = 'Compartir evento'")
                .contains("window.clearTimeout(flashTimer)")
                .contains("error.name !== 'AbortError'")
                .contains("fallbackCopy(url)");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
