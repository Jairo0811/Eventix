package com.jairomatias.eventix.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionRegistrationForm(
        @NotBlank(message = "El nombre del centro educativo es obligatorio.")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres.")
        String name,
        @NotBlank(message = "El código institucional es obligatorio.")
        @Size(max = 50, message = "El código no puede superar 50 caracteres.")
        String code) {
}
