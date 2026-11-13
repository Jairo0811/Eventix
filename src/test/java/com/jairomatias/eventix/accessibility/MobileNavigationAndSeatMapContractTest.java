package com.jairomatias.eventix.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MobileNavigationAndSeatMapContractTest {

    @Test
    void mobileMenuExposesItsStateAndHasAnAccessibleCloseControl() throws IOException {
        String topbar = resource("templates/fragments/navbar.html");
        String sidebar = resource("templates/fragments/sidebar.html");
        String css = resource("static/css/responsive.css");

        assertThat(topbar)
                .contains("data-sidebar-toggle")
                .contains("aria-controls=\"eventix-sidebar\"")
                .contains("aria-expanded=\"false\"");
        assertThat(sidebar)
                .contains("<aside id=\"eventix-sidebar\"")
                .contains("data-sidebar-close aria-label=\"Cerrar menú\"");
        assertThat(css)
                .contains("body.sidebar-open::before")
                .contains(".sidebar-close-button");
    }

    @Test
    void sidebarKeepsHiddenLinksOutOfTheTabOrderAndClosesWithEscape() throws IOException {
        String app = resource("static/js/app.js");

        assertThat(app)
                .contains("sidebar.inert = mobileLayout.matches && !open")
                .contains("sidebar.setAttribute(\"aria-hidden\", \"true\")")
                .contains("menuButton?.setAttribute(\"aria-expanded\", String(open))")
                .contains("event.key === \"Escape\"")
                .contains("event.key !== \"Tab\"")
                .contains("setSidebarOpen(false, true)")
                .contains("mobileLayout.addEventListener(\"change\"");
    }

    @Test
    void seatMapCanScrollHorizontallyAndKeepsEdgeSeatsWithinAPaddedPlane() throws IOException {
        String seats = resource("templates/checkout/seats.html");
        String css = resource("static/css/app.css");

        assertThat(seats)
                .contains("seat-map-scroll border rounded-4")
                .contains("role=\"region\" tabindex=\"0\"")
                .contains("aria-describedby=\"seat-map-instructions\"")
                .contains("class=\"seat-map-plane\"")
                .contains("class=\"seat-map-option border rounded-3")
                .contains("th:style=\"|left:${seat.xPosition}%; top:${seat.yPosition}%;|\"");
        assertThat(css)
                .contains(".seat-map-scroll {")
                .contains("overflow-x: auto;")
                .contains("min-width: 880px;")
                .contains(".seat-map-plane {")
                .contains("left: 58px;")
                .contains("right: 58px;");
    }

    @Test
    void seatCheckboxIncludesFullSemanticIdentity() throws IOException {
        String seats = resource("templates/checkout/seats.html");

        assertThat(seats).contains("aria-label=|Asiento ${seat.label}, fila ${seat.rowCode}, sección ${seat.sectionName}");
        assertThat(seats).contains("th:disabled=\"${seat.status.name() != 'AVAILABLE' or holdToken != null}\"");
    }

    @Test
    void mobileSeatMapInitiallyRevealsAnAvailableSeat() throws IOException {
        String seats = resource("templates/checkout/seats.html");
        String script = resource("static/js/seat-map-initial-view.js");

        assertThat(seats)
                .contains("data-seat-map-scroll role=\"region\"")
                .contains("@{/js/seat-map-initial-view.js}");
        assertThat(script)
                .contains("requestAnimationFrame(")
                .contains("map.scrollWidth <= map.clientWidth + 1")
                .contains("!input.disabled")
                .contains("map.scrollLeft = Math.max(");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
