package com.jairomatias.eventix.venue.dto;

import com.jairomatias.eventix.venue.entity.VenueSectionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VenueSectionForm {

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 40, message = "El código no puede superar 40 caracteres.")
    private String code;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String name;

    @NotNull(message = "El tipo de sección es obligatorio.")
    private VenueSectionType sectionType = VenueSectionType.RESERVED_SEATING;

    @Min(value = 1, message = "La capacidad debe ser mayor que cero.")
    private int capacity = 1;

    @Min(value = 0, message = "El orden no puede ser negativo.")
    private int sortOrder;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public VenueSectionType getSectionType() { return sectionType; }
    public void setSectionType(VenueSectionType sectionType) { this.sectionType = sectionType; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
