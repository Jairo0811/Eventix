package com.jairomatias.eventix.venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;
import com.jairomatias.eventix.venue.entity.VenueSeat;

class EventSeatInventoryEntityTest {

    @Test
    void holdExpiresAndCanBeReleased() {
        Event event = Mockito.mock(Event.class);
        VenueSeat seat = Mockito.mock(VenueSeat.class);
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
    void blockClearsTemporaryHoldAndOwner() {
        EventSeatInventory inventory = new EventSeatInventory(
                Mockito.mock(Event.class),
                Mockito.mock(VenueSeat.class));
        User buyer = Mockito.mock(User.class);
        ReflectionTestUtils.setField(buyer, "id", 7L);

        inventory.hold("hold", LocalDateTime.now().plusMinutes(10), buyer);
        inventory.block();

        assertEquals(EventSeatStatus.BLOCKED, inventory.getStatus());
        assertNull(inventory.getHoldToken());
        assertNull(inventory.getHoldExpiresAt());
        assertNull(inventory.getHeldByUser());
    }
}
