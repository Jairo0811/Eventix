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
                .contains("Descubre eventos y nuevas experiencias.")
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
    void netlifyV140PromotesOfficialTrailerWithoutEnablingUnverifiedLinks()
            throws IOException {
        String landing = read("netlify/index.html");
        String links = read("netlify/public-links.js");
        String hosting = read("netlify.toml");

        assertThat(landing)
                .contains("Eventix v1.4.0")
                .contains("En desarrollo")
                .contains("Sitio en preparación")
                .contains("id=\"trailer\"")
                .contains("src=\"/images/eventix-trailer-poster.jpg\"")
                .contains("id=\"eventix-trailer-link\"")
                .contains("id=\"eventix-app-link\"")
                .contains("src=\"/public-links.js\"")
                .contains("noindex, nofollow")
                .contains("Sin compras habilitadas")
                .doesNotContain("https://eventix.onrender.com")
                .doesNotContain("<form");

        assertThat(Files.exists(Path.of("netlify/images/eventix-trailer-poster.jpg")))
                .isTrue();
        assertThat(links)
                .contains("trailer: \"\"")
                .contains("application: \"\"")
                .contains("parsed.protocol !== \"https:\"")
                .doesNotContain("fetch(")
                .doesNotContain("localStorage")
                .doesNotContain("sessionStorage");

        assertThat(hosting)
                .contains("publish = \"netlify\"")
                .contains("script-src 'self'");
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

    @Test
    void stagingBlueprintUsesProductionGuardsButNeverProductionResources()
            throws IOException {
        String staging = read("render.staging.yaml");
        String production = read("render.production.yaml");
        String sqlInspection = read("scripts/sqlserver-staging-readiness.sql");

        assertThat(staging)
                .contains("name: eventix-staging")
                .contains("SPRING_PROFILES_ACTIVE")
                .contains("value: prod")
                .contains("autoDeployTrigger: off")
                .contains("EVENTIX_CHECKOUT_ENABLED")
                .contains("EVENTIX_ALLOW_DEFAULT_BOOTSTRAP_ADMIN")
                .contains("TICKETING_ALLOW_EPHEMERAL_SIGNING_KEY")
                .contains("FLYWAY_DB_USERNAME")
                .contains("mountPath: /app/data")
                .contains("value: false")
                .doesNotContain("jdbc:h2:")
                .doesNotContain("eventix-production")
                .doesNotContain("eventix-covers\n");
        assertThat(production)
                .contains("name: eventix-production")
                .doesNotContain("name: eventix-staging");
        assertThat(sqlInspection)
                .contains("SELECT")
                .contains("flyway_schema_history")
                .contains("payment_reconciliation_cases")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DELETE FROM");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
