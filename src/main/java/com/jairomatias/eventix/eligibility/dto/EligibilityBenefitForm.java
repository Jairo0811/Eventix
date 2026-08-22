package com.jairomatias.eventix.eligibility.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EligibilityBenefitForm(
        @NotNull(message = "El tipo de beneficio es obligatorio.")
        EligibilityBenefitType benefitType,
        @DecimalMin(value = "0.00", inclusive = false, message = "El valor del descuento debe ser mayor que cero.")
        BigDecimal discountValue,
        @Min(value = 1, message = "El límite de entradas debe ser mayor que cero.")
        Integer maxTicketsPerPurchase,
        @Min(value = 1, message = "El inventario reservado debe ser mayor que cero.")
        Integer reservedInventory,
        Long ticketTypeId,
        LocalDateTime earlyAccessAt) {
}
