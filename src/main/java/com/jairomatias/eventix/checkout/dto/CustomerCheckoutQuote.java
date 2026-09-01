package com.jairomatias.eventix.checkout.dto;

import java.math.BigDecimal;

public record CustomerCheckoutQuote(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String currency) {
}
