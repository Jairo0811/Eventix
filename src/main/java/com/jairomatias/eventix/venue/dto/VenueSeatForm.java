package com.jairomatias.eventix.venue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VenueSeatForm {

    @NotBlank(message = "El número de asiento es obligatorio.")
    @Size(max = 20, message = "El número no puede superar 20 caracteres.")
    private String seatNumber;

    @NotBlank(message = "La etiqueta es obligatoria.")
    @Size(max = 80, message = "La etiqueta no puede superar 80 caracteres.")
    private String label;

    private boolean accessible;
    private boolean companionSeat;

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isAccessible() { return accessible; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }
    public boolean isCompanionSeat() { return companionSeat; }
    public void setCompanionSeat(boolean companionSeat) { this.companionSeat = companionSeat; }
}
