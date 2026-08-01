package com.jairomatias.eventix.payment.dto;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;

import jakarta.validation.constraints.NotNull;

public class PaymentForm {

    @NotNull(message = "Selecciona el proveedor de pago.")
    private PaymentProvider provider;

    @NotNull(message = "Selecciona el resultado de la simulación.")
    private SimulationOutcome simulationOutcome = SimulationOutcome.APPROVE;

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public SimulationOutcome getSimulationOutcome() {
        return simulationOutcome;
    }

    public void setSimulationOutcome(SimulationOutcome simulationOutcome) {
        this.simulationOutcome = simulationOutcome;
    }
}
