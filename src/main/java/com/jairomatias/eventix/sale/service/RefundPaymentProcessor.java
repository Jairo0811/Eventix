package com.jairomatias.eventix.sale.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;

@Component
public class RefundPaymentProcessor {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;
    private static final int MAX_MESSAGE_LENGTH = 300;

    private final PaymentTransactionRepository paymentRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final TransactionReferenceGenerator referenceGenerator;

    public RefundPaymentProcessor(
            PaymentTransactionRepository paymentRepository,
            PaymentGatewayRegistry gatewayRegistry,
            TransactionReferenceGenerator referenceGenerator) {
        this.paymentRepository = paymentRepository;
        this.gatewayRegistry = gatewayRegistry;
        this.referenceGenerator = referenceGenerator;
    }

    public PaymentResult process(
            Sale sale,
            User actor,
            BigDecimal amount,
            String reason,
            LocalDateTime processedAt) {
        PaymentTransaction originalCharge = findOriginalCharge(sale.getId());
        PaymentResult result = executeGatewayRefund(sale, originalCharge, amount);
        saveRefundTransaction(
                sale,
                actor,
                originalCharge,
                amount,
                reason,
                result,
                processedAt);
        return result;
    }

    private PaymentTransaction findOriginalCharge(Long saleId) {
        return paymentRepository
                .findFirstBySale_IdAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                        saleId,
                        PaymentTransactionType.CHARGE,
                        PaymentStatus.APPROVED)
                .orElseThrow(() -> new BusinessRuleException(
                        "La venta no tiene un cobro aprobado para reembolsar."));
    }

    private PaymentResult executeGatewayRefund(
            Sale sale,
            PaymentTransaction originalCharge,
            BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return new PaymentResult(
                    PaymentStatus.APPROVED,
                    "NO-COST-REFUND",
                    "Reembolso sin movimiento monetario.");
        }
        PaymentCommand command = new PaymentCommand(
                sale.getReferenceCode(),
                originalCharge.getProvider(),
                PaymentTransactionType.REFUND,
                amount,
                sale.getCurrency(),
                SimulationOutcome.APPROVE);
        return gatewayRegistry.resolve(originalCharge.getProvider()).process(command);
    }

    private void saveRefundTransaction(
            Sale sale,
            User actor,
            PaymentTransaction originalCharge,
            BigDecimal amount,
            String reason,
            PaymentResult result,
            LocalDateTime processedAt) {
        paymentRepository.save(new PaymentTransaction(
                sale,
                nextPaymentReference(),
                originalCharge.getProvider(),
                PaymentTransactionType.REFUND,
                result.status(),
                amount,
                sale.getCurrency(),
                result.externalReference(),
                truncate(reason + " — " + result.message(), MAX_MESSAGE_LENGTH),
                processedAt,
                actor));
    }

    private String nextPaymentReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String value = referenceGenerator.generatePaymentReference();
            if (!paymentRepository.existsByTransactionReference(value)) {
                return value;
            }
        }
        throw new BusinessRuleException("No fue posible generar la referencia del reembolso.");
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
