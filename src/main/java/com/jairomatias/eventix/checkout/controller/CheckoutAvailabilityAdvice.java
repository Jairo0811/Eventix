package com.jairomatias.eventix.checkout.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Public pages must not offer purchases while the production gate is closed. */
@ControllerAdvice
public class CheckoutAvailabilityAdvice {

    @Value("${eventix.payments.checkout-enabled:true}")
    private boolean checkoutEnabled = true;

    @ModelAttribute("eventixCheckoutEnabled")
    public boolean checkoutEnabled() {
        return checkoutEnabled;
    }
}
