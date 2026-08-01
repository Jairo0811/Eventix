package com.jairomatias.eventix.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SaleActionForm {

    @NotBlank(message = "Indica el motivo de la operación.")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres.")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
