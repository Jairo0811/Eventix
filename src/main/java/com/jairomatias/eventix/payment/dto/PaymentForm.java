package com.jairomatias.eventix.payment.dto;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PaymentForm {

    @NotNull(message = "Selecciona el proveedor de pago.")
    private PaymentProvider provider;

    @NotNull(message = "Selecciona el resultado de la simulación.")
    private SimulationOutcome simulationOutcome = SimulationOutcome.APPROVE;

    @Size(max = 20000, message = "El token de pago excede el tamaño permitido.")
    private String walletToken;

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

    public String getWalletToken() {
        return walletToken;
    }

    public void setWalletToken(String walletToken) {
        this.walletToken = walletToken;
    }
}
