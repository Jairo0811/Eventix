package com.jairomatias.eventix.eligibility.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SchoolPromotionForm(
        @NotNull(message = "La institución es obligatoria.")
        Long institutionId,
        @NotBlank(message = "El nombre de la promoción es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
        String name,
        @Min(value = 1900, message = "El año de graduación no es válido.")
        @Max(value = 2200, message = "El año de graduación no es válido.")
        int graduationYear) {
}
