package com.jairomatias.eventix.sale.dto;

public record ReservationSaleOption(
        Long id,
        String referenceCode,
        Long eventId,
        String eventTitle,
        String attendeeName,
        int quantity) {
}
