package com.eventix.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EventDetailMapTemplateTest {

    @Test
    void eventDetailIncludesReadOnlyGoogleMapsPreview() throws IOException {
        var resource = new ClassPathResource("templates/events/detail.html");
        var template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("data-google-maps-preview")
                .contains("data-google-maps-frame")
                .contains("data-google-maps-placeholder")
                .contains("data-google-maps-status")
                .contains("data-google-maps-open")
                .contains("th:value=\"${event.venue}\"")
                .contains("th:value=\"${event.address}\"")
                .contains("th:value=\"${event.googleMapsUrl}\"")
                .contains("Ubicación del evento")
                .contains("Abrir en Maps");
    }
}
