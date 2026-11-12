package com.jairomatias.eventix.checkout.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.jairomatias.eventix.event.entity.EventSeatingMode;

public record CustomerCheckoutPage(
        Long eventId,
        String title,
        String venue,
        LocalDateTime startAt,
        String coverImageUrl,
        EventSeatingMode seatingMode,
        List<CustomerTicketOption> ticketTypes) {
}
