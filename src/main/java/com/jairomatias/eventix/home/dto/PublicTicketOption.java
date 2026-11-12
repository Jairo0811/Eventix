package com.jairomatias.eventix.home.dto;

import java.math.BigDecimal;

/** Tipo de entrada mostrado en la página pública de un evento. */
public record PublicTicketOption(
        String name,
        String category,
        BigDecimal price,
        int available) {

    /** Por debajo de este número se avisa al comprador de que quedan pocas. */
    public static final int LOW_STOCK_THRESHOLD = 20;

    public boolean soldOut() {
        return available <= 0;
    }

    public boolean lowStock() {
        return available > 0 && available <= LOW_STOCK_THRESHOLD;
    }
}
