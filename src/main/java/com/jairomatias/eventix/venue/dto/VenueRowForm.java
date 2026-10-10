package com.jairomatias.eventix.venue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VenueRowForm {

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 40, message = "El código no puede superar 40 caracteres.")
    private String code;

    @NotBlank(message = "La etiqueta es obligatoria.")
    @Size(max = 80, message = "La etiqueta no puede superar 80 caracteres.")
    private String label;

    @Min(value = 0, message = "El orden no puede ser negativo.")
    private int sortOrder;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
