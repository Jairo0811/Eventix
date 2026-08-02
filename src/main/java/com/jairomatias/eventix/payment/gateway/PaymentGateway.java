package com.jairomatias.eventix.payment.gateway;

import com.jairomatias.eventix.payment.entity.PaymentProvider;

public interface PaymentGateway {

    boolean supports(PaymentProvider provider);

    PaymentResult process(PaymentCommand command);
}
