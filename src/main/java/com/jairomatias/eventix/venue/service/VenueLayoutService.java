package com.jairomatias.eventix.venue.service;

import java.math.BigDecimal;

import com.jairomatias.eventix.venue.dto.VenueLayoutView;
import com.jairomatias.eventix.venue.dto.VenueRowForm;
import com.jairomatias.eventix.venue.dto.VenueSeatForm;
import com.jairomatias.eventix.venue.dto.VenueSectionForm;

public interface VenueLayoutService {

    VenueLayoutView getLayout(Long venueId);

    Long addSection(Long venueId, VenueSectionForm form);

    Long addRow(Long venueId, Long sectionId, VenueRowForm form);

    Long addSeat(Long venueId, Long sectionId, Long rowId, VenueSeatForm form);

    void updateSeatPosition(
            Long venueId,
            Long sectionId,
            Long rowId,
            Long seatId,
            BigDecimal xPosition,
            BigDecimal yPosition);
}
