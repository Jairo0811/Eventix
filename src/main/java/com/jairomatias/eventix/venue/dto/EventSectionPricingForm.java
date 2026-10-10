package com.jairomatias.eventix.venue.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class EventSectionPricingForm {

    private Long pricingId;

    @NotNull(message = "Selecciona una sección.")
    private Long sectionId;

    @NotNull(message = "Selecciona un tipo de entrada.")
    private Long ticketTypeId;

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "Usa un importe válido con hasta dos decimales.")
    private BigDecimal priceOverride;

    public Long getPricingId() {
        return pricingId;
    }

    public void setPricingId(Long pricingId) {
        this.pricingId = pricingId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }
}
