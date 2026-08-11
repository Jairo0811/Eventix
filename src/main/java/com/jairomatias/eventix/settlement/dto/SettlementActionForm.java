package com.jairomatias.eventix.settlement.dto;

import jakarta.validation.constraints.Size;

public class SettlementActionForm {

    @Size(max = 120, message = "La referencia admite hasta 120 caracteres.")
    private String externalReference;

    @Size(max = 1000, message = "Las observaciones admiten hasta 1000 caracteres.")
    private String administrativeNotes;

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getAdministrativeNotes() {
        return administrativeNotes;
    }

    public void setAdministrativeNotes(String administrativeNotes) {
        this.administrativeNotes = administrativeNotes;
    }
}
