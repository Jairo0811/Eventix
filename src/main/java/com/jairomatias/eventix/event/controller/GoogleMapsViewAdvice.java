package com.jairomatias.eventix.event.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = EventController.class)
public class GoogleMapsViewAdvice {

    private final String embedApiKey;

    public GoogleMapsViewAdvice(
            @Value("${eventix.maps.google.embed-api-key:}")
            String embedApiKey) {
        this.embedApiKey = embedApiKey;
    }

    @ModelAttribute("googleMapsEmbedApiKey")
    public String googleMapsEmbedApiKey() {
        return embedApiKey;
    }
}
