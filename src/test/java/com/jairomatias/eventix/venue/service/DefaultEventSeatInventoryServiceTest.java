package com.jairomatias.eventix.venue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.dto.SeatHoldResult;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;
import com.jairomatias.eventix.venue.entity.VenueRow;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.repository.EventSeatInventoryRepository;
import com.jairomatias.eventix.venue.repository.VenueSeatRepository;

@ExtendWith(MockitoExtension.class)
class DefaultEventSeatInventoryServiceTest {

    private static final Long EVENT_ID = 10L;

    @Mock private EventRepository eventRepository;
    @Mock private VenueSeatRepository seatRepository;
    @Mock private EventSeatInventoryRepository inventoryRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private UserRepository userRepository;

    private DefaultEventSeatInventoryService service;
    private Event event;

    @BeforeEach
    void setUp() {
        event = mock(Event.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-09T18:00:00Z"),
                ZoneOffset.UTC);
        service = new DefaultEventSeatInventoryService(
                eventRepository,
                seatRepository,
                inventoryRepository,
                saleRepository,
                userRepository,
                clock);
        when(eventRepository.existsById(EVENT_ID)).thenReturn(true);
    }

    @Test
    void bestAvailableSelectsContiguousCenterSeats() {
        VenueRow row = row(101L);
        EventSeatInventory seat1 = inventory(1L, row, "1", "1.0", false, false);
        EventSeatInventory seat2 = inventory(2L, row, "2", "2.0", false, false);
        EventSeatInventory seat3 = inventory(3L, row, "3", "3.0", false, false);
        EventSeatInventory seat4 = inventory(4L, row, "4", "4.0", false, false);
        EventSeatInventory seat5 = inventory(5L, row, "5", "5.0", false, false);
        when(inventoryRepository.findAllForBestAvailableForUpdate(EVENT_ID))
                .thenReturn(List.of(seat1, seat2, seat3, seat4, seat5));

        SeatHoldResult result = service.holdBestAvailableSeats(EVENT_ID, 2, false);

        assertThat(result.seatIds()).containsExactly(2L, 3L);
        assertThat(seat2.getStatus()).isEqualTo(EventSeatStatus.HELD);
        assertThat(seat3.getStatus()).isEqualTo(EventSeatStatus.HELD);
        assertThat(seat2.getHoldToken()).isEqualTo(result.holdToken());
        assertThat(seat3.getHoldToken()).isEqualTo(result.holdToken());
        assertThat(seat1.getStatus()).isEqualTo(EventSeatStatus.AVAILABLE);
        assertThat(seat4.getStatus()).isEqualTo(EventSeatStatus.AVAILABLE);
    }

    @Test
    void bestAvailableDoesNotJumpAcrossUnavailableSeat() {
        VenueRow row = row(201L);
        EventSeatInventory seat1 = inventory(11L, row, "1", "1.0", false, false);
        EventSeatInventory seat2 = inventory(12L, row, "2", "2.0", false, false);
        EventSeatInventory seat3 = inventory(13L, row, "3", "3.0", false, false);
        EventSeatInventory seat4 = inventory(14L, row, "4", "4.0", false, false);
        seat2.block();
        when(inventoryRepository.findAllForBestAvailableForUpdate(EVENT_ID))
                .thenReturn(List.of(seat1, seat2, seat3, seat4));

        SeatHoldResult result = service.holdBestAvailableSeats(EVENT_ID, 2, false);

        assertThat(result.seatIds()).containsExactly(13L, 14L);
        assertThat(seat1.getStatus()).isEqualTo(EventSeatStatus.AVAILABLE);
        assertThat(seat2.getStatus()).isEqualTo(EventSeatStatus.BLOCKED);
    }

    @Test
    void bestAvailableAccessibleRequestUsesAccessibleAndCompanionBlock() {
        VenueRow row = row(301L);
        EventSeatInventory accessible = inventory(21L, row, "1", "1.0", true, false);
        EventSeatInventory companion = inventory(22L, row, "2", "2.0", false, true);
        EventSeatInventory standard = inventory(23L, row, "3", "3.0", false, false);
        when(inventoryRepository.findAllForBestAvailableForUpdate(EVENT_ID))
                .thenReturn(List.of(accessible, companion, standard));

        SeatHoldResult result = service.holdBestAvailableSeats(EVENT_ID, 2, true);

        assertThat(result.seatIds()).containsExactly(21L, 22L);
        assertThat(accessible.getStatus()).isEqualTo(EventSeatStatus.HELD);
        assertThat(companion.getStatus()).isEqualTo(EventSeatStatus.HELD);
        assertThat(standard.getStatus()).isEqualTo(EventSeatStatus.AVAILABLE);
    }

    @Test
    void bestAvailablePreservesAccessibleInventoryForStandardRequests() {
        VenueRow row = row(401L);
        EventSeatInventory accessible = inventory(31L, row, "1", "1.0", true, false);
        EventSeatInventory companion = inventory(32L, row, "2", "2.0", false, true);
        EventSeatInventory standard1 = inventory(33L, row, "3", "3.0", false, false);
        EventSeatInventory standard2 = inventory(34L, row, "4", "4.0", false, false);
        when(inventoryRepository.findAllForBestAvailableForUpdate(EVENT_ID))
                .thenReturn(List.of(accessible, companion, standard1, standard2));

        SeatHoldResult result = service.holdBestAvailableSeats(EVENT_ID, 2, false);

        assertThat(result.seatIds()).containsExactly(33L, 34L);
        assertThat(accessible.getStatus()).isEqualTo(EventSeatStatus.AVAILABLE);
        assertThat(companion.getStatus()).isEqualTo(EventSeatStatus.AVAILABLE);
    }

    @Test
    void bestAvailableRejectsWhenNoContiguousStandardBlockExists() {
        VenueRow row = row(501L);
        EventSeatInventory seat1 = inventory(41L, row, "1", "1.0", false, false);
        EventSeatInventory seat2 = inventory(42L, row, "2", "2.0", false, false);
        EventSeatInventory seat3 = inventory(43L, row, "3", "3.0", false, false);
        seat2.block();
        when(inventoryRepository.findAllForBestAvailableForUpdate(EVENT_ID))
                .thenReturn(List.of(seat1, seat2, seat3));

        assertThatThrownBy(() -> service.holdBestAvailableSeats(EVENT_ID, 2, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloque contiguo");
    }

    @Test
    void bestAvailableValidatesQuantityBeforeLockingInventory() {
        assertThatThrownBy(() -> service.holdBestAvailableSeats(EVENT_ID, 11, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entre 1 y 10");
    }

    private VenueRow row(Long id) {
        VenueRow row = mock(VenueRow.class);
        when(row.getId()).thenReturn(id);
        return row;
    }

    private EventSeatInventory inventory(
            Long seatId,
            VenueRow row,
            String seatNumber,
            String xPosition,
            boolean accessible,
            boolean companion) {
        VenueSeat seat = mock(VenueSeat.class);
        when(seat.getId()).thenReturn(seatId);
        when(seat.getRow()).thenReturn(row);
        when(seat.getSeatNumber()).thenReturn(seatNumber);
        when(seat.getXPosition()).thenReturn(new BigDecimal(xPosition));
        when(seat.isAccessible()).thenReturn(accessible);
        when(seat.isCompanionSeat()).thenReturn(companion);
        return new EventSeatInventory(event, seat);
    }
}
