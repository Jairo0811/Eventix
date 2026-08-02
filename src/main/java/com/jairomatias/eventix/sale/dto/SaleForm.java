package com.jairomatias.eventix.sale.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SaleForm {

    private static final int DEFAULT_LINE_COUNT = 6;

    @NotNull(message = "Selecciona una reservación confirmada.")
    private Long reservationId;

    @Valid
    @Size(min = 1, max = 6, message = "La venta admite hasta seis tipos de entrada.")
    private List<SaleLineForm> items = defaultItems();

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public List<SaleLineForm> getItems() {
        return items;
    }

    public void setItems(List<SaleLineForm> items) {
        this.items = items;
    }

    private static List<SaleLineForm> defaultItems() {
        List<SaleLineForm> lines = new ArrayList<>();
        for (int index = 0; index < DEFAULT_LINE_COUNT; index++) {
            lines.add(new SaleLineForm());
        }
        return lines;
    }
}
