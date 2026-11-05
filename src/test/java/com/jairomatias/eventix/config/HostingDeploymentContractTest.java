package com.jairomatias.eventix.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** Guards the separation of the static landing, demo and production app. */
class HostingDeploymentContractTest {

    @Test
    void netlifyServesOnlyStaticInformationalContent() throws IOException {
        String config = read("netlify.toml");
        String landing = read("netlify/index.html");

        assertThat(config).contains("publish = \"netlify\"");
        assertThat(landing)
                .contains("<html lang=\"es\">")
                .contains("name=\"viewport\"")
                .contains("noindex, nofollow")
                .contains("Sitio en preparación")
                .contains("https://github.com/Jairo0811/Eventix")
                .doesNotContain("<form")
                .doesNotContain("/tickets/")
                .doesNotContain("https://eventix.onrender.com");
    }

    @Test
    void publicDemoMatchesBuyerStorefrontWithoutFakeTransactions() throws IOException {
        String landing = read("netlify/index.html");
        String script = read("netlify/demo.js");
        String config = read("netlify.toml");

        assertThat(landing)
                .contains("Compra boletas para tus eventos favoritos.")
                .contains("Eventix Tech Experience 2026")
                .contains("src=\"/images/eventix-logo.png\"")
                .contains("src=\"/images/eventix-tech-experience-2026.svg\"")
                .contains("id=\"demo-search\"")
                .contains("id=\"demo-category\"")
                .contains("id=\"demo-dialog\"")
                .contains("data-demo-event")
                .contains("data-demo-details")
                .contains("demo.js")
                .contains("Sin compras habilitadas")
                .doesNotContain("<form")
                .doesNotContain("/login")
                .doesNotContain("/checkout")
                .doesNotContain("/tickets/");

        assertThat(Files.exists(Path.of("netlify/images/eventix-logo.png"))).isTrue();
        assertThat(Files.exists(Path.of("netlify/images/eventix-tech-experience-2026.svg"))).isTrue();
        assertThat(config).contains("script-src 'self'");
        assertThat(script)
                .contains("data-demo-event")
                .contains("dialog.showModal()")
                .doesNotContain("fetch(")
                .doesNotContain("XMLHttpRequest")
                .doesNotContain("localStorage")
                .doesNotContain("sessionStorage");
    }

    @Test
    void productionBlueprintDoesNotInheritDemoDatabaseOrEphemeralKeys() throws IOException {
        String demo = read("src/main/resources/application-render.yml");
        String prod = read("render.production.yaml");

        assertThat(demo).contains("jdbc:h2:mem:eventix");
        assertThat(prod)
                .contains("SPRING_PROFILES_ACTIVE")
                .contains("value: prod")
                .contains("autoDeployTrigger: off")
                .contains("FLYWAY_DB_USERNAME")
                .contains("TICKETING_SIGNING_PRIVATE_KEY")
                .contains("TICKETING_ALLOW_EPHEMERAL_SIGNING_KEY")
                .contains("mountPath: /app/data")
                .doesNotContain("jdbc:h2:mem:")
                .doesNotContain("create-drop")
                .doesNotContain("value: render");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
