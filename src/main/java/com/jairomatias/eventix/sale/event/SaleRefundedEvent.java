package com.jairomatias.eventix.sale.event;

public record SaleRefundedEvent(
        Long saleId,
        String reason) {
}
