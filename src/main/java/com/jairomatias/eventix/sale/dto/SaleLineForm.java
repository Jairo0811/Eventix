package com.jairomatias.eventix.sale.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SaleLineForm {

    private Long ticketTypeId;

    @Min(value = 0, message = "La cantidad no puede ser negativa.")
    @Max(value = 100, message = "Una línea no puede superar 100 entradas.")
    private int quantity;

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
