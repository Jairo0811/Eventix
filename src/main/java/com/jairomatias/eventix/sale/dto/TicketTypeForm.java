package com.jairomatias.eventix.sale.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.sale.entity.TicketTypeCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TicketTypeForm {

    @NotNull(message = "Selecciona una categoría de entrada.")
    private TicketTypeCategory category = TicketTypeCategory.GENERAL;

    @NotBlank(message = "El nombre del tipo de entrada es obligatorio.")
    @Size(max = 80, message = "El nombre no puede exceder 80 caracteres.")
    private String name;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
    private BigDecimal price = BigDecimal.ZERO;

    @Min(value = 1, message = "La capacidad debe ser mayor que cero.")
    @Max(value = 1000000, message = "La capacidad indicada es demasiado alta.")
    private int capacity = 1;

    private boolean active = true;

    public TicketTypeCategory getCategory() {
        return category;
    }

    public void setCategory(TicketTypeCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
