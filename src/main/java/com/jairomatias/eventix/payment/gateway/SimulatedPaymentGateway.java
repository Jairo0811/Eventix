package com.jairomatias.eventix.payment.gateway;

import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public boolean supports(PaymentProvider provider) {
        return provider != null && !provider.isDigitalWallet();
    }

    @Override
    public PaymentResult process(PaymentCommand command) {
        boolean approved = command.simulationOutcome()
                != SimulationOutcome.DECLINE;
        PaymentStatus status = approved
                ? PaymentStatus.APPROVED
                : PaymentStatus.DECLINED;
        String externalReference = "SIM-"
                + UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 20)
                        .toUpperCase();
        String message = approved
                ? "Operación simulada aprobada."
                : "Operación simulada rechazada.";

        return new PaymentResult(status, externalReference, message);
    }
}
