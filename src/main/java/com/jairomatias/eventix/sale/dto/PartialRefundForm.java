package com.jairomatias.eventix.sale.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class PartialRefundForm {

    @NotEmpty(message = "Selecciona al menos una boleta para reembolsar.")
    private List<Long> ticketIds = new ArrayList<>();

    @NotBlank(message = "Indica el motivo del reembolso.")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres.")
    private String reason;

    public List<Long> getTicketIds() {
        return ticketIds;
    }

    public void setTicketIds(List<Long> ticketIds) {
        this.ticketIds = ticketIds == null ? new ArrayList<>() : new ArrayList<>(ticketIds);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
