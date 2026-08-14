package com.jairomatias.eventix.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FinalAccessibilityPassTest {

    @Test
    void finalLayerIsLoadedAfterContrastLayer() throws IOException {
        String head = resource("templates/fragments/head.html");

        int contrast = head.indexOf("/css/theme-contrast.css");
        int finalLayer = head.indexOf("/css/final-a11y.css");

        assertThat(contrast).isGreaterThanOrEqualTo(0);
        assertThat(finalLayer).isGreaterThan(contrast);
    }

    @Test
    void finalLayerCoversPublicQrReportsErrorsAndForcedColors() throws IOException {
        String css = resource("static/css/final-a11y.css");

        assertThat(css)
                .contains(".home-page")
                .contains(".auth-panel")
                .contains(".report-card")
                .contains(".scanner-card")
                .contains(".scan-result-valid")
                .contains(".scan-result-rejected")
                .contains(".ticket-qr")
                .contains("img[alt*=\"QR\"]")
                .contains("@media (forced-colors: active)");
    }

    @Test
    void accessControlExposesQrStateValidationAndTableSemantics() throws IOException {
        String access = resource("templates/access/index.html");

        assertThat(access)
                .contains("<main class=\"app-main\" id=\"main-content\"")
                .contains("aria-labelledby=\"access-title\"")
                .contains("role=\"status\" aria-live=\"polite\"")
                .contains("th:aria-invalid=\"${#fields.hasErrors('token')}\"")
                .contains("id=\"token-error\" role=\"alert\"")
                .contains("id=\"reentry-help\"")
                .contains("aria-atomic=\"true\"")
                .contains("<caption class=\"visually-hidden\"")
                .contains("<th scope=\"col\">Fecha</th>")
                .contains("aria-current=\"page\"");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
