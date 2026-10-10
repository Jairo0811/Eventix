package com.jairomatias.eventix.venue.dto;

import com.jairomatias.eventix.event.entity.EventSeatingMode;

import jakarta.validation.constraints.NotNull;

public class EventVenueConfigurationForm {

    private Long venueId;

    @NotNull(message = "Selecciona un modo de asientos.")
    private EventSeatingMode seatingMode = EventSeatingMode.GENERAL_ADMISSION;

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }
    public EventSeatingMode getSeatingMode() { return seatingMode; }
    public void setSeatingMode(EventSeatingMode seatingMode) { this.seatingMode = seatingMode; }
}
