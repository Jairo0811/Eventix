package com.jairomatias.eventix.eligibility.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class SchoolAlumniBenefitForm {

    @NotNull
    private Boolean enabled = true;

    private Long schoolPromotionId;

    private EligibilityBenefitType discountType =
            EligibilityBenefitType.PERCENTAGE_DISCOUNT;

    @DecimalMin(
            value = "0.01",
            message = "El descuento debe ser mayor que cero.")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "El descuento admite hasta 10 enteros y 2 decimales.")
    private BigDecimal discountValue;

    public static SchoolAlumniBenefitForm from(
            SchoolAlumniBenefitConfiguration configuration) {
        SchoolAlumniBenefitForm form = new SchoolAlumniBenefitForm();
        form.setEnabled(configuration.enabled());
        form.setSchoolPromotionId(configuration.schoolPromotionId());
        form.setDiscountType(configuration.discountType());
        form.setDiscountValue(configuration.discountValue());
        return form;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getSchoolPromotionId() {
        return schoolPromotionId;
    }

    public void setSchoolPromotionId(Long schoolPromotionId) {
        this.schoolPromotionId = schoolPromotionId;
    }

    public EligibilityBenefitType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(EligibilityBenefitType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }
}
