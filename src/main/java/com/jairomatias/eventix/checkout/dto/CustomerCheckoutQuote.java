package com.jairomatias.eventix.checkout.dto;

import java.math.BigDecimal;

/**
 * Resultado de una cotización.
 *
 * <p>{@code quantity} es la cantidad que realmente se cotizó. Con asientos
 * reservados manda la retención, así que puede diferir de la que pidió el
 * cliente; devolverla permite que la interfaz muestre el mismo número que se
 * cobrará.
 */
public record CustomerCheckoutQuote(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String currency,
        int quantity) {
}
