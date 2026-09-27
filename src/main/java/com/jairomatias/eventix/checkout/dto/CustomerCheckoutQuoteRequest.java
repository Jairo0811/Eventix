package com.jairomatias.eventix.checkout.dto;

public record CustomerCheckoutQuoteRequest(
        Long ticketTypeId,
        int quantity,
        String couponCode) {
}
