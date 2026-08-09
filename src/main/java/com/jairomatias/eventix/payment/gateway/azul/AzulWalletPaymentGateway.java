package com.jairomatias.eventix.payment.gateway.azul;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGateway;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AzulWalletPaymentGateway implements PaymentGateway {

    private final AzulWalletProperties properties;
    private final AzulSoapClient client;
    private final PaymentTransactionRepository paymentRepository;

    public AzulWalletPaymentGateway(
            AzulWalletProperties properties,
            AzulSoapClient client,
            PaymentTransactionRepository paymentRepository) {
        this.properties = properties;
        this.client = client;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public boolean supports(PaymentProvider provider) {
        return properties.isReady()
                && provider != null
                && provider.isDigitalWallet();
    }

    @Override
    public PaymentResult process(PaymentCommand command) {
        PaymentCommand effectiveCommand = command.transactionType()
                == PaymentTransactionType.REFUND
                        ? refundCommand(command)
                        : command;
        AzulSoapClient.AzulPaymentResponse response =
                client.process(effectiveCommand);
        boolean approved = "00".equals(response.isoCode());
        String message = firstText(
                response.responseMessage(),
                response.errorDescription(),
                approved ? "Operación aprobada por Azul." : "Operación rechazada por Azul.");
        return new PaymentResult(
                approved ? PaymentStatus.APPROVED : PaymentStatus.DECLINED,
                response.azulOrderId(),
                message);
    }

    private PaymentCommand refundCommand(PaymentCommand command) {
        String originalReference = command.originalExternalReference();
        if (!StringUtils.hasText(originalReference)) {
            originalReference = paymentRepository
                    .findFirstBySale_ReferenceCodeAndProviderAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                            command.saleReference(),
                            command.provider(),
                            PaymentTransactionType.CHARGE,
                            PaymentStatus.APPROVED)
                    .map(transaction -> transaction.getExternalReference())
                    .filter(StringUtils::hasText)
                    .orElseThrow(() -> new BusinessRuleException(
                            "No se encontró el AzulOrderId del cobro original."));
        }
        return new PaymentCommand(
                command.saleReference(),
                command.provider(),
                command.transactionType(),
                command.amount(),
                command.currency(),
                command.simulationOutcome(),
                null,
                originalReference);
    }

    private static String firstText(
            String first,
            String second,
            String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return fallback;
    }
}
