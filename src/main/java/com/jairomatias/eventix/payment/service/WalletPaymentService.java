package com.jairomatias.eventix.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.sale.service.TransactionReferenceGenerator;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

import org.springframework.context.ApplicationEventPublisher;

@Service
public class WalletPaymentService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final TransactionReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public WalletPaymentService(
            SaleRepository saleRepository,
            UserRepository userRepository,
            PaymentTransactionRepository paymentRepository,
            PaymentGatewayRegistry gatewayRegistry,
            TransactionReferenceGenerator referenceGenerator,
            ApplicationEventPublisher eventPublisher) {
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.gatewayRegistry = gatewayRegistry;
        this.referenceGenerator = referenceGenerator;
        this.eventPublisher = eventPublisher;
        this.clock = Clock.systemDefaultZone();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public boolean charge(
            Long saleId,
            PaymentProvider provider,
            String walletToken,
            String authenticatedLogin) {
        if (provider == null || !provider.isDigitalWallet()) {
            throw new BusinessRuleException(
                    "Selecciona Apple Pay o Google Pay.");
        }
        if (!StringUtils.hasText(walletToken)) {
            throw new BusinessRuleException(
                    "La billetera digital no devolvió un token de pago válido.");
        }
        User actor = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        authenticatedLogin,
                        authenticatedLogin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
        Sale sale = saleRepository.findDetailedByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));
        ensurePayable(sale);

        PaymentCommand command = new PaymentCommand(
                sale.getReferenceCode(),
                provider,
                PaymentTransactionType.CHARGE,
                sale.getTotal(),
                sale.getCurrency(),
                SimulationOutcome.APPROVE,
                walletToken,
                null);
        PaymentResult result = gatewayRegistry.resolve(provider).process(command);
        LocalDateTime processedAt = LocalDateTime.now(clock);
        paymentRepository.save(new PaymentTransaction(
                sale,
                nextPaymentReference(),
                provider,
                PaymentTransactionType.CHARGE,
                result.status(),
                sale.getTotal(),
                sale.getCurrency(),
                result.externalReference(),
                result.message(),
                processedAt,
                actor));

        if (result.status() != PaymentStatus.APPROVED) {
            return false;
        }
        sale.markPaid(processedAt);
        eventPublisher.publishEvent(new SalePaidEvent(sale.getId()));
        return true;
    }

    private void ensurePayable(Sale sale) {
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new BusinessRuleException(
                    "Solo las ventas pendientes pueden pagarse.");
        }
        if (!sale.getEvent().getStartAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException(
                    "No se puede pagar una venta de un evento que ya inició.");
        }
    }

    private String nextPaymentReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String reference = referenceGenerator.generatePaymentReference();
            if (!paymentRepository.existsByTransactionReference(reference)) {
                return reference;
            }
        }
        throw new BusinessRuleException(
                "No fue posible generar una referencia de pago única.");
    }
}
