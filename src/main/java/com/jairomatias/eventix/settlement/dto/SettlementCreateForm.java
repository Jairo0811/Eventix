package com.jairomatias.eventix.settlement.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SettlementCreateForm {

    @NotNull(message = "Selecciona un organizador.")
    private Long organizerId;

    @NotNull(message = "Indica el inicio del período.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodFrom;

    @NotNull(message = "Indica el final del período.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodTo;

    @Size(max = 1000, message = "Las observaciones admiten hasta 1000 caracteres.")
    private String administrativeNotes;

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public void setPeriodFrom(LocalDate periodFrom) {
        this.periodFrom = periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public void setPeriodTo(LocalDate periodTo) {
        this.periodTo = periodTo;
    }

    public String getAdministrativeNotes() {
        return administrativeNotes;
    }

    public void setAdministrativeNotes(String administrativeNotes) {
        this.administrativeNotes = administrativeNotes;
    }
}
