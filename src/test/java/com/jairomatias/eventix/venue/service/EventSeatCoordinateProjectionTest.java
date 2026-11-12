package com.jairomatias.eventix.venue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.dto.EventSeatView;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.VenueRow;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.entity.VenueSection;
import com.jairomatias.eventix.venue.repository.EventSeatInventoryRepository;
import com.jairomatias.eventix.venue.repository.VenueSeatRepository;

class EventSeatCoordinateProjectionTest {

    @Test
    void inventoryViewCarriesSeatMapCoordinates() {
        EventRepository eventRepository = mock(EventRepository.class);
        VenueSeatRepository seatRepository = mock(VenueSeatRepository.class);
        EventSeatInventoryRepository inventoryRepository = mock(EventSeatInventoryRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        DefaultEventSeatInventoryService service = new DefaultEventSeatInventoryService(
                eventRepository,
                seatRepository,
                inventoryRepository,
                saleRepository,
                userRepository,
                Clock.systemUTC());

        Event event = mock(Event.class);
        VenueSection section = mock(VenueSection.class);
        VenueRow row = mock(VenueRow.class);
        VenueSeat seat = mock(VenueSeat.class);

        when(eventRepository.existsById(10L)).thenReturn(true);
        when(section.getCode()).thenReturn("VIP");
        when(section.getName()).thenReturn("VIP Central");
        when(row.getCode()).thenReturn("A");
        when(row.getSection()).thenReturn(section);
        when(seat.getId()).thenReturn(25L);
        when(seat.getRow()).thenReturn(row);
        when(seat.getSeatNumber()).thenReturn("12");
        when(seat.getLabel()).thenReturn("A-12");
        when(seat.getXPosition()).thenReturn(new BigDecimal("37.5000"));
        when(seat.getYPosition()).thenReturn(new BigDecimal("64.2500"));

        EventSeatInventory inventory = new EventSeatInventory(event, seat);
        when(inventoryRepository
                .findAllByEvent_IdOrderBySeat_Row_Section_SortOrderAscSeat_Row_SortOrderAscSeat_SeatNumberAsc(10L))
                .thenReturn(List.of(inventory));

        EventSeatView view = service.getInventory(10L).get(0);

        assertThat(view.seatId()).isEqualTo(25L);
        assertThat(view.xPosition()).isEqualByComparingTo("37.5000");
        assertThat(view.yPosition()).isEqualByComparingTo("64.2500");
    }
}
