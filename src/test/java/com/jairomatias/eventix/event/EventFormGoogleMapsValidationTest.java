package com.jairomatias.eventix.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.dto.EventForm;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class EventFormGoogleMapsValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void acceptsGoogleMapsShareUrl() {
        EventForm form = new EventForm();
        form.setGoogleMapsUrl("https://maps.app.goo.gl/AbCdEf123");

        assertThat(mapsViolations(form)).isEmpty();
    }

    @Test
    void rejectsNonGoogleMapsUrl() {
        EventForm form = new EventForm();
        form.setGoogleMapsUrl("https://example.com/location");

        assertThat(mapsViolations(form)).hasSize(1);
    }

    private Set<ConstraintViolation<EventForm>> mapsViolations(
            EventForm form) {
        return validator.validate(form).stream()
                .filter(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("googleMapsUrl"))
                .collect(java.util.stream.Collectors.toSet());
    }
}
