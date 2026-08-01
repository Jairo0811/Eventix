package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;

public record SaleItemView(
        Long id,
        Long ticketTypeId,
        String ticketTypeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
