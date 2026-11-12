package com.jairomatias.eventix.venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.venue.entity.Venue;
import com.jairomatias.eventix.venue.entity.VenueRow;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.entity.VenueSection;
import com.jairomatias.eventix.venue.entity.VenueSectionType;

class VenueLayoutEntityTest {

    @Test
    void buildsReusableReservedSeatingLayout() {
        Venue venue = new Venue(
                "Arena Eventix",
                "Av. Principal 1",
                "Santo Domingo",
                "DO",
                "America/Santo_Domingo");

        VenueSection section = new VenueSection(
                venue,
                "VIP-A",
                "VIP A",
                VenueSectionType.RESERVED_SEATING,
                100,
                1);

        VenueRow row = new VenueRow(section, "A", "Fila A", 1);

        VenueSeat seat = new VenueSeat(
                row,
                "12",
                "A-12",
                true,
                false);

        seat.update(
                "12",
                "A-12",
                true,
                false,
                new BigDecimal("14.2500"),
                new BigDecimal("8.5000"),
                true);

        assertEquals("Arena Eventix", venue.getName());
        assertEquals("VIP-A", section.getCode());
        assertEquals(VenueSectionType.RESERVED_SEATING, section.getSectionType());
        assertEquals("A", row.getCode());
        assertEquals("12", seat.getSeatNumber());
        assertTrue(seat.isAccessible());
        assertFalse(seat.isCompanionSeat());
        assertEquals(new BigDecimal("14.2500"), seat.getXPosition());
        assertEquals(new BigDecimal("8.5000"), seat.getYPosition());
    }
}
