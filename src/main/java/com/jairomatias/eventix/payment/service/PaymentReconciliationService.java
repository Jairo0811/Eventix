package com.jairomatias.eventix.payment.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentWebhookInbox;
import com.jairomatias.eventix.payment.entity.PaymentReconciliationCase;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.payment.repository.PaymentWebhookInboxRepository;
import com.jairomatias.eventix.payment.repository.PaymentReconciliationCaseRepository;

/**
 * Records discrepancies between independently verified provider facts and
 * Eventix's payment ledger. This service NEVER changes sale/payment state.
 *
 * Not exposed via HTTP. Calling code must first verify the provider's official
 * signature and replay window and persist a matching verified inbox entry.
 */
@Service
public class PaymentReconciliationService {

    private final PaymentWebhookInboxRepository inbox;
    private final PaymentReconciliationCaseRepository reviews;
    private final PaymentTransactionRepository transactions;

    public PaymentReconciliationService(
            PaymentWebhookInboxRepository inbox,
            PaymentReconciliationCaseRepository reviews,
            PaymentTransactionRepository transactions) {
        this.inbox = inbox;
        this.reviews = reviews;
        this.transactions = transactions;
    }

    @Transactional
    public boolean reviewPreviouslyVerified(VerifiedReconciliationEvidence evidence) {
        if (evidence == null) {
            throw new IllegalArgumentException("Faltan los datos de conciliación.");
        }

        PaymentWebhookInbox received = inbox.findByProviderAndExternalEventId(
                        evidence.provider(), evidence.eventId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una notificación bancaria verificada."));

        if (!"VERIFIED".equals(received.getStatus())
                || !received.getPayloadSha256().equalsIgnoreCase(evidence.payloadSha256())) {
            throw new IllegalArgumentException(
                    "La notificación bancaria no coincide con el recibo verificado.");
        }

        if (reviews.existsByProviderAndExternalEventId(
                evidence.provider(), evidence.eventId())) {
            return false;
        }

        String reason = compareWithLocalLedger(evidence);
        String outcome = "MATCHED".equals(reason) ? "MATCHED" : "REVIEW_REQUIRED";
        reviews.saveAndFlush(new PaymentReconciliationCase(
                evidence.provider(),
                evidence.eventId(),
                evidence.saleReference(),
                evidence.type(),
                evidence.externalReference(),
                evidence.status(),
                evidence.amount(),
                evidence.currency(),
                outcome,
                reason,
                LocalDateTime.now(ZoneId.of("America/Santo_Domingo"))));
        // Unique(provider,eventId) enforces replay resistance even under races.
        return true;
    }

    private String compareWithLocalLedger(VerifiedReconciliationEvidence evidence) {
        if (evidence.status() != PaymentStatus.APPROVED) {
            return "BANK_DECLINED";
        }

        Optional<PaymentTransaction> local = transactions
                .findFirstBySale_ReferenceCodeAndProviderAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                        evidence.saleReference(),
                        evidence.provider(),
                        evidence.type(),
                        PaymentStatus.APPROVED);

        if (local.isEmpty()) {
            return "NO_LOCAL_APPROVAL";
        }
        PaymentTransaction transaction = local.get();
        if (!evidence.externalReference().equals(transaction.getExternalReference())) {
            return "REFERENCE_MISMATCH";
        }
        if (evidence.amount().compareTo(transaction.getAmount()) != 0) {
            return "AMOUNT_MISMATCH";
        }
        if (!evidence.currency().equals(transaction.getCurrency())) {
            return "CURRENCY_MISMATCH";
        }
        return "MATCHED";
    }
}
