package com.jairomatias.eventix.venue.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public class SeatSelectionForm {

    @NotEmpty(message = "Selecciona al menos un asiento.")
    private List<Long> seatIds = new ArrayList<>();

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}
