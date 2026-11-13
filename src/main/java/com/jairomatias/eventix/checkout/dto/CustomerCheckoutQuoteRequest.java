package com.jairomatias.eventix.checkout.dto;

/**
 * Petición de cotización del checkout.
 *
 * <p>{@code holdToken} es obligatorio cuando el tipo de entrada exige asientos
 * reservados: en ese caso la cantidad que manda es la de los asientos
 * efectivamente retenidos, no la que venga en {@code quantity}. Sin él, la
 * cotización podría mostrar un importe distinto del que cobrará la compra.
 */
public record CustomerCheckoutQuoteRequest(
        Long ticketTypeId,
        int quantity,
        String couponCode,
        String holdToken) {
}
