package com.jairomatias.eventix.venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;
import com.jairomatias.eventix.venue.entity.VenueSeat;

class EventSeatInventoryEntityTest {

    @Test
    void holdExpiresAndCanBeReleased() {
        Event event = Mockito.mock(Event.class);
        VenueSeat seat = Mockito.mock(VenueSeat.class);
        EventSeatInventory inventory = new EventSeatInventory(event, seat);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 20, 20, 10);

        inventory.hold("abc123", expiresAt);

        assertEquals(EventSeatStatus.HELD, inventory.getStatus());
        assertEquals("abc123", inventory.getHoldToken());
        assertEquals(expiresAt, inventory.getHoldExpiresAt());
        assertFalse(inventory.isHeldAndExpired(expiresAt.minusSeconds(1)));
        assertTrue(inventory.isHeldAndExpired(expiresAt));

        inventory.release();

        assertEquals(EventSeatStatus.AVAILABLE, inventory.getStatus());
        assertNull(inventory.getHoldToken());
        assertNull(inventory.getHoldExpiresAt());
    }

    @Test
    void blockClearsTemporaryHold() {
        EventSeatInventory inventory = new EventSeatInventory(
                Mockito.mock(Event.class),
                Mockito.mock(VenueSeat.class));

        inventory.hold("hold", LocalDateTime.now().plusMinutes(10));
        inventory.block();

        assertEquals(EventSeatStatus.BLOCKED, inventory.getStatus());
        assertNull(inventory.getHoldToken());
        assertNull(inventory.getHoldExpiresAt());
    }
}
