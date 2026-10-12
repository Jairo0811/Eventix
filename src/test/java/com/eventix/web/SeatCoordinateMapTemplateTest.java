package com.eventix.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SeatCoordinateMapTemplateTest {

    @Test
    void checkoutRendersCoordinateMapAndFallbackSelector() throws IOException {
        var resource = new ClassPathResource("templates/checkout/seats.html");
        var template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("hasPositionedSeats")
                .contains("hasUnpositionedSeats")
                .contains("seat.xPosition")
                .contains("seat.yPosition")
                .contains("Mapa de asientos")
                .contains("Asientos sin posición de mapa");
    }

    @Test
    void adminLayoutIncludesCoordinatePreviewAndEditor() throws IOException {
        var resource = new ClassPathResource("templates/venues/layout.html");
        var template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Vista previa del mapa")
                .contains("xPosition")
                .contains("yPosition")
                .contains("/position")
                .contains("ESCENARIO");
    }
}
