package com.jairomatias.eventix.venue.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @DecimalMin(value = "0.0", message = "La posición X debe estar entre 0 y 100.")
    @DecimalMax(value = "100.0", message = "La posición X debe estar entre 0 y 100.")
    @Digits(integer = 3, fraction = 4, message = "La posición X admite hasta 4 decimales.")
    private BigDecimal xPosition;

    @DecimalMin(value = "0.0", message = "La posición Y debe estar entre 0 y 100.")
    @DecimalMax(value = "100.0", message = "La posición Y debe estar entre 0 y 100.")
    @Digits(integer = 3, fraction = 4, message = "La posición Y admite hasta 4 decimales.")
    private BigDecimal yPosition;

    @AssertTrue(message = "Indica ambas coordenadas X e Y, o deja ambas vacías.")
    public boolean isCoordinatePairValid() {
        return (xPosition == null) == (yPosition == null);
    }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isAccessible() { return accessible; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }
    public boolean isCompanionSeat() { return companionSeat; }
    public void setCompanionSeat(boolean companionSeat) { this.companionSeat = companionSeat; }
    public BigDecimal getXPosition() { return xPosition; }
    public void setXPosition(BigDecimal xPosition) { this.xPosition = xPosition; }
    public BigDecimal getYPosition() { return yPosition; }
    public void setYPosition(BigDecimal yPosition) { this.yPosition = yPosition; }
}
