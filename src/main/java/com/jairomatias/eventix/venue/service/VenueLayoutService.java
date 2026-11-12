package com.jairomatias.eventix.venue.service;

import com.jairomatias.eventix.venue.dto.VenueLayoutView;
import com.jairomatias.eventix.venue.dto.VenueRowForm;
import com.jairomatias.eventix.venue.dto.VenueSeatForm;
import com.jairomatias.eventix.venue.dto.VenueSectionForm;

public interface VenueLayoutService {

    VenueLayoutView getLayout(Long venueId);

    Long addSection(Long venueId, VenueSectionForm form);

    Long addRow(Long venueId, Long sectionId, VenueRowForm form);

    Long addSeat(Long venueId, Long sectionId, Long rowId, VenueSeatForm form);
}
