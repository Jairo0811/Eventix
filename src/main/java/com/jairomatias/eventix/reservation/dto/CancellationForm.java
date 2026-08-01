package com.jairomatias.eventix.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancellationForm {

    @NotBlank(message = "Indica el motivo de la cancelación.")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres.")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
