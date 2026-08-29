package com.jairomatias.eventix.eligibility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolInstitutionForm(
        @NotBlank(message = "El nombre de la institución es obligatorio.")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres.")
        String name,
        @NotBlank(message = "El código de la institución es obligatorio.")
        @Size(max = 50, message = "El código no puede superar 50 caracteres.")
        String code) {
}
