package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.sale.entity.SaleStatus;

public record SaleListItem(
        Long id,
        String referenceCode,
        Long eventId,
        String eventTitle,
        String reservationReference,
        String buyerName,
        String buyerEmail,
        SaleStatus status,
        BigDecimal total,
        String currency,
        LocalDateTime createdAt) {
}
