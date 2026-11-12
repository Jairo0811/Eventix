package com.jairomatias.eventix.venue.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.jairomatias.eventix.venue.dto.EventSeatView;
import com.jairomatias.eventix.venue.dto.SeatHoldResult;

public interface EventSeatInventoryService {

    int initializeInventory(Long eventId, String authenticatedLogin);

    List<EventSeatView> getInventory(Long eventId);

    List<EventSeatView> getInventory(Long eventId, Long requiredSectionId);

    Optional<SeatHoldResult> getActiveHold(Long eventId, String authenticatedLogin);

    SeatHoldResult holdSeats(
            Long eventId,
            Collection<Long> seatIds,
            Long requiredSectionId,
            String authenticatedLogin);

    SeatHoldResult holdBestAvailableSeats(
            Long eventId,
            int quantity,
            boolean accessibilityRequired,
            Long requiredSectionId,
            String authenticatedLogin);

    int validateActiveHold(
            Long eventId,
            String holdToken,
            String authenticatedLogin);

    int validateActiveHold(
            Long eventId,
            String holdToken,
            Long requiredSectionId,
            String authenticatedLogin);

    void releaseHold(
            Long eventId,
            String holdToken,
            String authenticatedLogin);

    void confirmSale(
            Long eventId,
            String holdToken,
            Long saleId,
            String authenticatedLogin);

    int releaseExpiredHolds();
}
