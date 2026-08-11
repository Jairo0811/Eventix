package com.jairomatias.eventix.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;

import com.jairomatias.eventix.promotion.entity.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CouponForm {

    @NotBlank(message = "Indica el código del cupón.")
    @Size(max = 40, message = "El código admite hasta 40 caracteres.")
    @Pattern(
            regexp = "[A-Za-z0-9_-]+",
            message = "Usa solo letras, números, guiones y guiones bajos.")
    private String code;

    @NotBlank(message = "Indica una descripción.")
    @Size(max = 240, message = "La descripción admite hasta 240 caracteres.")
    private String description;

    @NotNull(message = "Selecciona el tipo de descuento.")
    private DiscountType discountType;

    @NotNull(message = "Indica el valor del descuento.")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero.")
    private BigDecimal value;

    @NotNull(message = "Indica la fecha de inicio.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startsAt;

    @NotNull(message = "Indica la fecha de expiración.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiresAt;

    private boolean active = true;

    @Min(value = 1, message = "El límite total debe ser mayor que cero.")
    private Integer totalUseLimit;

    @Min(value = 1, message = "El límite por usuario debe ser mayor que cero.")
    private Integer perUserLimit;

    @DecimalMin(
            value = "0.00",
            message = "El importe mínimo no puede ser negativo.")
    private BigDecimal minimumSubtotal;

    @Size(min = 1, message = "Selecciona al menos un evento.")
    private Set<Long> eventIds = new LinkedHashSet<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getTotalUseLimit() {
        return totalUseLimit;
    }

    public void setTotalUseLimit(Integer totalUseLimit) {
        this.totalUseLimit = totalUseLimit;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public BigDecimal getMinimumSubtotal() {
        return minimumSubtotal;
    }

    public void setMinimumSubtotal(BigDecimal minimumSubtotal) {
        this.minimumSubtotal = minimumSubtotal;
    }

    public Set<Long> getEventIds() {
        return eventIds;
    }

    public void setEventIds(Set<Long> eventIds) {
        this.eventIds = eventIds;
    }
}
