package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.jairomatias.eventix.payment.dto.PaymentTransactionView;
import com.jairomatias.eventix.sale.entity.SaleStatus;

public record SaleDetailsView(
        Long id,
        String referenceCode,
        Long reservationId,
        String reservationReference,
        Long eventId,
        String eventTitle,
        LocalDateTime eventStartAt,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        SaleStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        String refundReason,
        LocalDateTime cancelledAt,
        String cancellationReason,
        String soldByName,
        LocalDateTime createdAt,
        List<SaleItemView> items,
        List<PaymentTransactionView> payments) {
}
