package com.jairomatias.eventix.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DashboardResponsiveContractTest {

    @Test
    void dashboardOffersRoleScopedAnchorsToExistingSections() throws IOException {
        String dashboard = resource("templates/dashboard/index.html");

        assertThat(dashboard)
                .contains("class=\"dashboard-jump-nav\" aria-label=\"Ir a secciones del panel\" sec:authorize=\"hasRole('ORGANIZER')\"")
                .contains("class=\"dashboard-jump-nav\" aria-label=\"Ir a secciones del panel\" sec:authorize=\"hasRole('ADMINISTRATOR')\"")
                .contains("href=\"#organizer-commercial-title\"")
                .contains("href=\"#organizer-events-title\"")
                .contains("href=\"#organizer-finance-title\"")
                .contains("href=\"#executive-title\"")
                .contains("href=\"#performance-title\"")
                .contains("href=\"#operations-title\"")
                .contains("href=\"#users-title\"")
                .contains("href=\"#quick-actions-title\"");
    }

    @Test
    void dashboardFinancialAndEventTablesExposeNamesAndKeyboardScrolling() throws IOException {
        String dashboard = resource("templates/dashboard/index.html");

        assertThat(dashboard)
                .contains("aria-label=\"Próximos eventos del organizador\"")
                .contains("aria-describedby=\"organizer-events-table-hint\"")
                .contains("aria-label=\"Eventos con mayor rendimiento\"")
                .contains("aria-describedby=\"top-events-table-hint\"")
                .contains("class=\"table-responsive dashboard-table-scroll\" role=\"region\" tabindex=\"0\"")
                .contains("<caption class=\"visually-hidden\">");
    }

    @Test
    void reportTablesKeepTheirExistingDataAndBecomeKeyboardScrollable() throws IOException {
        String reports = resource("templates/reports/index.html");

        assertThat(reports)
                .contains("aria-labelledby=\"events-report-title\" aria-describedby=\"events-report-table-hint\"")
                .contains("aria-labelledby=\"category-report-title\" aria-describedby=\"category-report-table-hint\"")
                .contains("aria-labelledby=\"monthly-report-title\" aria-describedby=\"monthly-report-table-hint\"")
                .contains("aria-labelledby=\"organizer-report-title\" aria-describedby=\"organizer-report-table-hint\"")
                .contains("th:each=\"row : ${report.byEvent}\"")
                .contains("th:each=\"row : ${report.byCategory}\"")
                .contains("th:each=\"row : ${report.monthlyRevenue}\"")
                .contains("th:each=\"row : ${report.byOrganizer}\"");
        assertThat(reports.split("role=\"region\" tabindex=\"0\"", -1)).hasSize(5);
    }

    @Test
    void dashboardCssKeepsLinksAndFinancialValuesReadable() throws IOException {
        String css = resource("static/css/dashboard.css");

        assertThat(css)
                .contains(".dashboard-jump-nav a:focus-visible")
                .contains(".dashboard-section-heading h2[id]")
                .contains("scroll-margin-top: calc(var(--eventix-topbar-height) + 1.25rem)")
                .contains(".dashboard-table-scroll:focus-visible")
                .contains(".table-scroll-hint")
                .contains("font-variant-numeric: tabular-nums")
                .contains("@media (max-width: 575.98px)")
                .contains("grid-template-columns: minmax(0, 1fr);");
    }

    @Test
    void reportTablesKeepColumnNamesIntactAndShowEmptyStatesWithoutClipping() throws IOException {
        String reports = resource("templates/reports/index.html");
        String dashboard = resource("templates/dashboard/index.html");
        String css = resource("static/css/dashboard.css");

        assertThat(reports)
                .contains("th:unless=\"${#lists.isEmpty(report.byEvent)}\"")
                .contains("th:unless=\"${#lists.isEmpty(report.byCategory)}\"")
                .contains("th:unless=\"${#lists.isEmpty(report.monthlyRevenue)}\"")
                .contains("th:unless=\"${#lists.isEmpty(report.byOrganizer)}\"")
                .contains("No hay datos para el período seleccionado.")
                .contains("No hay resultados por categoría en el período seleccionado.")
                .contains("No hay ingresos mensuales en el período seleccionado.")
                .contains("No hay actividad por organizador en el período seleccionado.")
                .contains("class=\"table report-table-wide\"")
                .contains("class=\"table report-table-compact\"");
        assertThat(dashboard)
                .contains("th:unless=\"${#lists.isEmpty(summary.topEvents)}\"")
                .contains("Aún no hay actividad comercial este año.");
        assertThat(css)
                .contains(".dashboard-table-scroll .table thead th")
                .contains("white-space: nowrap;")
                .contains(".dashboard-table-scroll .table.report-table-wide")
                .contains("min-width: 1000px;")
                .contains(".dashboard-table-scroll .table.report-table-compact")
                .contains("min-width: 600px;")
                .contains(".dashboard-shell > section > .dashboard-panel {")
                .contains("height: auto;");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
