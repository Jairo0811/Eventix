package com.jairomatias.eventix.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ThemeContrastTest {

    @Test
    void sharedHeadLoadsContrastLayerAfterAccessibilityStyles() throws IOException {
        String head = resource("templates/fragments/head.html");

        int accessibility = head.indexOf("/css/accessibility.css");
        int contrast = head.indexOf("/css/theme-contrast.css");

        assertThat(accessibility).isGreaterThanOrEqualTo(0);
        assertThat(contrast).isGreaterThan(accessibility);
    }

    @Test
    void contrastLayerProtectsCommonBootstrapSurfacesInDarkMode() throws IOException {
        String css = resource("static/css/theme-contrast.css");

        assertThat(css)
                .contains("html[data-theme=\"dark\"]")
                .contains(".text-bg-light")
                .contains(".alert-light")
                .contains(".table-light")
                .contains(".text-success")
                .contains(".text-danger")
                .contains("input:-webkit-autofill")
                .contains(".btn-outline-primary")
                .contains(".btn-outline-danger");
    }

    @Test
    void contrastLayerDefinesReadableLightAndDarkSemanticTokens() throws IOException {
        String css = resource("static/css/theme-contrast.css");

        assertThat(css)
                .contains("--eventix-aa-text")
                .contains("--eventix-aa-muted")
                .contains("--eventix-aa-soft-bg")
                .contains("--eventix-aa-danger")
                .contains("--eventix-aa-success")
                .contains("--eventix-aa-warning")
                .contains("--eventix-aa-info");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
