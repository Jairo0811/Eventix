package com.jairomatias.eventix.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway =
            new SimulatedPaymentGateway();

    @Test
    void approvesRequestedSimulation() {
        PaymentResult result = gateway.process(new PaymentCommand(
                "SAL-ABCDEFGH2345",
                PaymentProvider.AZUL,
                PaymentTransactionType.CHARGE,
                new BigDecimal("1500.00"),
                "DOP",
                SimulationOutcome.APPROVE));

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.externalReference()).startsWith("SIM-");
    }

    @Test
    void declinesRequestedSimulation() {
        PaymentResult result = gateway.process(new PaymentCommand(
                "SAL-ABCDEFGH2345",
                PaymentProvider.CARDNET,
                PaymentTransactionType.CHARGE,
                new BigDecimal("1500.00"),
                "DOP",
                SimulationOutcome.DECLINE));

        assertThat(result.status()).isEqualTo(PaymentStatus.DECLINED);
    }
}
