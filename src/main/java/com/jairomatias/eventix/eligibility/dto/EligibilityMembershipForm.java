package com.jairomatias.eventix.eligibility.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EligibilityMembershipForm(
        @NotBlank(message = "El correo del miembro es obligatorio.")
        @Email(message = "Indica un correo válido.")
        String email) {
}
