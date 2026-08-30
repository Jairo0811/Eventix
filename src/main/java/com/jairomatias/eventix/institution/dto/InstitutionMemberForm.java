package com.jairomatias.eventix.institution.dto;

import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InstitutionMemberForm(
        @NotBlank(message = "El correo del usuario es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String email,
        @NotNull(message = "El rol institucional es obligatorio.")
        InstitutionMembershipRole role) {
}
