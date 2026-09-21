package com.jairomatias.eventix.venue.service;

import java.util.List;

import com.jairomatias.eventix.venue.dto.EventVenueConfigurationForm;
import com.jairomatias.eventix.venue.dto.VenueOption;

public interface EventVenueConfigurationService {

    EventVenueConfigurationForm getForm(Long eventId, String authenticatedLogin);

    List<VenueOption> getActiveVenues();

    void configure(
            Long eventId,
            EventVenueConfigurationForm form,
            String authenticatedLogin);
}
