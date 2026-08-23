package com.jairomatias.eventix.sale.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentCommand;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.sale.dto.PartialRefundForm;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.event.SaleRefundedEvent;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.event.TicketPassChangedEvent;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class PartialRefundService {

    private static final int MONEY_SCALE = 2;
    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final SaleRepository saleRepository;
    private final DigitalTicketRepository ticketRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final TransactionReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public PartialRefundService(
            SaleRepository saleRepository,
            DigitalTicketRepository ticketRepository,
            PaymentTransactionRepository paymentRepository,
            UserRepository userRepository,
            PaymentGatewayRegistry gatewayRegistry,
            TransactionReferenceGenerator referenceGenerator,
            ApplicationEventPublisher eventPublisher) {
        this.saleRepository = saleRepository;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.gatewayRegistry = gatewayRegistry;
        this.referenceGenerator = referenceGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public BigDecimal refundTickets(
            Long saleId,
            PartialRefundForm form,
            String authenticatedLogin) {
        ValidatedRequest request = validateRequest(form);
        User actor = findActor(authenticatedLogin);
        Sale sale = findSaleForUpdate(saleId);
        ensureRefundable(sale);

        List<DigitalTicket> selected = ticketRepository.findAllBySaleIdAndIdsForUpdate(
                saleId, request.ticketIds());
        if (selected.size() != request.ticketIds().size()) {
            throw new BusinessRuleException("Una o más boletas seleccionadas no pertenecen a esta venta.");
        }
        selected.forEach(this::ensureTicketRefundable);

        List<DigitalTicket> allTickets = ticketRepository.findAllBySale_IdOrderBySequenceNumberAsc(saleId);
        long refundableCount = allTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE)
                .count();
        boolean completesRefund = selected.size() == refundableCount;
        BigDecimal refundAmount = calculateRefundAmount(sale, selected, completesRefund);

        PaymentTransaction originalCharge = findOriginalCharge(saleId);
        PaymentResult result = processRefund(sale, originalCharge, refundAmount);
        LocalDateTime processedAt = LocalDateTime.now();
        saveRefundTransaction(sale, actor, originalCharge, refundAmount, request.reason(), result, processedAt);

        if (result.status() != PaymentStatus.APPROVED) {
            throw new BusinessRuleException("La pasarela no aprobó el reembolso.");
        }

        sale.recordRefund(refundAmount, request.reason(), processedAt);
        cancelSelectedTickets(selected, request.reason(), processedAt);
        if (completesRefund) {
            sale.getReservation().cancel(request.reason(), processedAt);
            eventPublisher.publishEvent(new SaleRefundedEvent(sale.getId(), request.reason()));
        }
        return refundAmount;
    }

    private ValidatedRequest validateRequest(PartialRefundForm form) {
        if (form == null || form.getTicketIds() == null || form.getTicketIds().isEmpty()) {
            throw new BusinessRuleException("Selecciona al menos una boleta para reembolsar.");
        }
        String reason = form.getReason() == null ? "" : form.getReason().trim();
        if (reason.isEmpty()) {
            throw new BusinessRuleException("Indica el motivo del reembolso.");
        }
        if (reason.length() > 500) {
            throw new BusinessRuleException("El motivo no puede exceder 500 caracteres.");
        }
        Set<Long> ids = new HashSet<>(form.getTicketIds());
        if (ids.contains(null) || ids.size() != form.getTicketIds().size()) {
            throw new BusinessRuleException("La selección de boletas contiene valores inválidos o duplicados.");
        }
        return new ValidatedRequest(Set.copyOf(ids), reason);
    }

    private User findActor(String login) {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
    }

    private Sale findSaleForUpdate(Long saleId) {
        return saleRepository.findDetailedByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la venta solicitada."));
    }

    private void ensureRefundable(Sale sale) {
        if (sale.getStatus() != SaleStatus.PAID
                && sale.getStatus() != SaleStatus.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException("La venta no admite reembolsos en su estado actual.");
        }
        if (!sale.getEvent().getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("No se puede reembolsar una venta después de iniciado el evento.");
        }
    }

    private void ensureTicketRefundable(DigitalTicket ticket) {
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Solo se pueden reembolsar boletas activas que no hayan sido utilizadas ni canceladas.");
        }
    }

    private BigDecimal calculateRefundAmount(
            Sale sale,
            List<DigitalTicket> selected,
            boolean completesRefund) {
        if (completesRefund) {
            return sale.getRemainingAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (sale.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException(
                    "Una venta gratuita debe anularse completamente; no admite reembolso monetario parcial.");
        }
        BigDecimal selectedGross = selected.stream()
                .map(ticket -> ticket.getSaleItem().getUnitPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sale.getTotal()
                .multiply(selectedGross)
                .divide(sale.getSubtotal(), MONEY_SCALE, RoundingMode.HALF_UP)
                .min(sale.getRemainingAmount())
                .max(BigDecimal.ZERO)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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

    private PaymentResult processRefund(
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
                truncate(reason + " — " + result.message(), 300),
                processedAt,
                actor));
    }

    private void cancelSelectedTickets(
            List<DigitalTicket> tickets,
            String reason,
            LocalDateTime processedAt) {
        for (DigitalTicket ticket : tickets) {
            ticket.cancel(reason, processedAt);
            eventPublisher.publishEvent(new TicketPassChangedEvent(ticket.getId()));
        }
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

    private record ValidatedRequest(Set<Long> ticketIds, String reason) {
    }
}
