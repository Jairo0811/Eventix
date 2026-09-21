package com.jairomatias.eventix.venue.service;

import java.util.Collection;
import java.util.List;

import com.jairomatias.eventix.venue.dto.EventSeatView;
import com.jairomatias.eventix.venue.dto.SeatHoldResult;

public interface EventSeatInventoryService {

    int initializeInventory(Long eventId, String authenticatedLogin);

    List<EventSeatView> getInventory(Long eventId);

    SeatHoldResult holdSeats(Long eventId, Collection<Long> seatIds);

    int validateActiveHold(Long eventId, String holdToken);

    void releaseHold(Long eventId, String holdToken);

    void confirmSale(Long eventId, String holdToken, Long saleId);

    int releaseExpiredHolds();
}
