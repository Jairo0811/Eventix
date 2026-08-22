package com.jairomatias.eventix.eligibility.dto;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EligibilityGroupForm(
        @NotBlank(message = "El nombre del grupo es obligatorio.")
        @Size(max = 160, message = "El nombre no puede superar 160 caracteres.")
        String name,
        @NotNull(message = "El tipo de grupo es obligatorio.")
        EligibilityGroupType groupType,
        @Min(value = 0, message = "El límite de familiares no puede ser negativo.")
        Integer maxRelatedPeople) {
}
