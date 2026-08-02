package com.jairomatias.eventix.payment.gateway;

import java.util.List;

import org.springframework.stereotype.Component;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Component
public class PaymentGatewayRegistry {

    private final List<PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = List.copyOf(gateways);
    }

    public PaymentGateway resolve(PaymentProvider provider) {
        return gateways.stream()
                .filter(gateway -> gateway.supports(provider))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "No existe una pasarela disponible para el proveedor seleccionado."));
    }
}
