package com.jairomatias.eventix.checkout.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerCheckoutPage(
        Long eventId,
        String title,
        String venue,
        LocalDateTime startAt,
        String coverImageUrl,
        List<CustomerTicketOption> ticketTypes) {
}
