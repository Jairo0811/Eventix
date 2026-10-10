package com.jairomatias.eventix.venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;
import com.jairomatias.eventix.venue.entity.VenueRow;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.entity.VenueSection;

class EventSeatInventoryEntityTest {

    @Test
    void holdExpiresAndCanBeReleased() {
        Event event = Mockito.mock(Event.class);
        VenueSeat seat = seat("VIP", "VIP Norte", "A", "12", "A-12", false);
        User buyer = Mockito.mock(User.class);
        Mockito.when(buyer.getId()).thenReturn(91L);
        EventSeatInventory inventory = new EventSeatInventory(event, seat);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 20, 20, 10);

        inventory.hold("abc123", expiresAt, buyer);

        assertEquals(EventSeatStatus.HELD, inventory.getStatus());
        assertEquals("abc123", inventory.getHoldToken());
        assertEquals(expiresAt, inventory.getHoldExpiresAt());
        assertSame(buyer, inventory.getHeldByUser());
        assertTrue(inventory.isHeldBy(91L));
        assertFalse(inventory.isHeldBy(92L));
        assertFalse(inventory.isHeldAndExpired(expiresAt.minusSeconds(1)));
        assertTrue(inventory.isHeldAndExpired(expiresAt));

        inventory.release();

        assertEquals(EventSeatStatus.AVAILABLE, inventory.getStatus());
        assertNull(inventory.getHoldToken());
        assertNull(inventory.getHoldExpiresAt());
        assertNull(inventory.getHeldByUser());
    }

    @Test
    void capturesImmutableSeatIdentityWhenInventoryIsCreated() {
        VenueSeat seat = seat("VIP", "VIP Norte", "A", "12", "A-12", true);
        VenueSection section = seat.getRow().getSection();
        EventSeatInventory inventory = new EventSeatInventory(
                Mockito.mock(Event.class),
                seat);

        Mockito.when(section.getName()).thenReturn("VIP Renombrado");
        Mockito.when(seat.getLabel()).thenReturn("A-99");

        assertEquals("VIP", inventory.getSectionCodeSnapshot());
        assertEquals("VIP Norte", inventory.getSectionNameSnapshot());
        assertEquals("A", inventory.getRowCodeSnapshot());
        assertEquals("12", inventory.getSeatNumberSnapshot());
        assertEquals("A-12", inventory.getSeatLabelSnapshot());
        assertTrue(inventory.isAccessibleSnapshot());
    }

    @Test
    void blockClearsTemporaryHoldAndOwner() {
        EventSeatInventory inventory = new EventSeatInventory(
                Mockito.mock(Event.class),
                seat("PLATEA", "Platea", "B", "8", "B-8", false));
        User buyer = Mockito.mock(User.class);

        inventory.hold("hold", LocalDateTime.now().plusMinutes(10), buyer);
        inventory.block();

        assertEquals(EventSeatStatus.BLOCKED, inventory.getStatus());
        assertNull(inventory.getHoldToken());
        assertNull(inventory.getHoldExpiresAt());
        assertNull(inventory.getHeldByUser());
    }

    private VenueSeat seat(
            String sectionCode,
            String sectionName,
            String rowCode,
            String seatNumber,
            String seatLabel,
            boolean accessible) {
        VenueSection section = Mockito.mock(VenueSection.class);
        Mockito.when(section.getCode()).thenReturn(sectionCode);
        Mockito.when(section.getName()).thenReturn(sectionName);

        VenueRow row = Mockito.mock(VenueRow.class);
        Mockito.when(row.getSection()).thenReturn(section);
        Mockito.when(row.getCode()).thenReturn(rowCode);

        VenueSeat seat = Mockito.mock(VenueSeat.class);
        Mockito.when(seat.getRow()).thenReturn(row);
        Mockito.when(seat.getSeatNumber()).thenReturn(seatNumber);
        Mockito.when(seat.getLabel()).thenReturn(seatLabel);
        Mockito.when(seat.isAccessible()).thenReturn(accessible);
        return seat;
    }
}
