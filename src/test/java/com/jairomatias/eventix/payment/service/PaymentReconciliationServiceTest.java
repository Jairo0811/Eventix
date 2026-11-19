package com.jairomatias.eventix.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.entity.PaymentReconciliationCase;
import com.jairomatias.eventix.payment.entity.PaymentWebhookInbox;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.payment.repository.PaymentWebhookInboxRepository;
import com.jairomatias.eventix.payment.repository.PaymentReconciliationCaseRepository;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    private static final String DIGEST = "abc12345".repeat(8);

    @Mock private PaymentWebhookInboxRepository inbox;
    @Mock private PaymentReconciliationCaseRepository reviews;
    @Mock private PaymentTransactionRepository transactions;

    private PaymentReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentReconciliationService(inbox, reviews, transactions);
    }

    private VerifiedReconciliationEvidence evidence(
            PaymentStatus status, String reference, BigDecimal amount, String currency) {
        return new VerifiedReconciliationEvidence(
                PaymentProvider.AZUL, "bank_event_123", DIGEST,
                "SAL-ABCDEFGH2345", PaymentTransactionType.CHARGE,
                reference, status, amount, currency);
    }

    private PaymentWebhookInbox verifiedReceipt() {
        return new PaymentWebhookInbox(
                PaymentProvider.AZUL, "bank_event_123", DIGEST,
                java.time.LocalDateTime.of(2026, 11, 19, 10, 0));
    }

    private PaymentTransaction localPayment() {
        PaymentTransaction local = mock(PaymentTransaction.class);
        when(local.getExternalReference()).thenReturn("bank_payment_123");
        when(local.getAmount()).thenReturn(new BigDecimal("1500.00"));
        when(local.getCurrency()).thenReturn("DOP");
        return local;
    }

    private void stubVerifiedReceipt() {
        when(inbox.findByProviderAndExternalEventId(
                PaymentProvider.AZUL, "bank_event_123"))
                .thenReturn(Optional.of(verifiedReceipt()));
    }

    private void stubApprovedLocalPayment(PaymentTransaction local) {
        when(transactions
                .findFirstBySale_ReferenceCodeAndProviderAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                        "SAL-ABCDEFGH2345", PaymentProvider.AZUL,
                        PaymentTransactionType.CHARGE, PaymentStatus.APPROVED))
                .thenReturn(Optional.of(local));
    }

    private PaymentReconciliationCase savedCase() {
        ArgumentCaptor<PaymentReconciliationCase> captor =
                ArgumentCaptor.forClass(PaymentReconciliationCase.class);
        verify(reviews).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void identicalVerifiedApprovalIsRecordedAsMatchedWithoutMutation() {
        stubVerifiedReceipt();
        stubApprovedLocalPayment(localPayment());

        assertThat(service.reviewPreviouslyVerified(
                evidence(PaymentStatus.APPROVED, "bank_payment_123",
                        new BigDecimal("1500.00"), "DOP"))).isTrue();

        PaymentReconciliationCase recorded = savedCase();
        assertThat(recorded.getOutcome()).isEqualTo("MATCHED");
        assertThat(recorded.getReasonCode()).isEqualTo("MATCHED");
        assertThat(recorded.getReportedAmount()).isEqualByComparingTo("1500.00");
        assertThat(recorded.getReviewedAt()).isNotNull();
    }

    @Test
    void amountMismatchIsFlaggedForManualReview() {
        stubVerifiedReceipt();
        PaymentTransaction local = mock(PaymentTransaction.class);
        when(local.getExternalReference()).thenReturn("bank_payment_123");
        when(local.getAmount()).thenReturn(new BigDecimal("1500.00"));
        stubApprovedLocalPayment(local);

        service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1800.00"), "DOP"));

        assertThat(savedCase().getReasonCode()).isEqualTo("AMOUNT_MISMATCH");
    }

    @Test
    void currencyMismatchIsFlaggedForManualReview() {
        stubVerifiedReceipt();
        stubApprovedLocalPayment(localPayment());

        service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1500.00"), "USD"));

        assertThat(savedCase().getReasonCode()).isEqualTo("CURRENCY_MISMATCH");
    }

    @Test
    void referenceMismatchIsFlaggedForManualReview() {
        stubVerifiedReceipt();
        PaymentTransaction local = mock(PaymentTransaction.class);
        when(local.getExternalReference()).thenReturn("bank_payment_123");
        stubApprovedLocalPayment(local);

        service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "other_reference",
                new BigDecimal("1500.00"), "DOP"));

        assertThat(savedCase().getReasonCode()).isEqualTo("REFERENCE_MISMATCH");
    }

    @Test
    void unrecognizedExternalApprovalIsNeverAppliedToTheSale() {
        stubVerifiedReceipt();

        service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1500.00"), "DOP"));

        PaymentReconciliationCase recorded = savedCase();
        assertThat(recorded.getOutcome()).isEqualTo("REVIEW_REQUIRED");
        assertThat(recorded.getReasonCode()).isEqualTo("NO_LOCAL_APPROVAL");
    }

    @Test
    void bankDeclineRemainsForManualReview() {
        stubVerifiedReceipt();

        service.reviewPreviouslyVerified(evidence(
                PaymentStatus.DECLINED, "bank_payment_123",
                new BigDecimal("1500.00"), "DOP"));

        assertThat(savedCase().getReasonCode()).isEqualTo("BANK_DECLINED");
        verifyNoInteractions(transactions);
    }

    @Test
    void replayDoesNotCreateASecondReview() {
        stubVerifiedReceipt();
        when(reviews.existsByProviderAndExternalEventId(
                PaymentProvider.AZUL, "bank_event_123")).thenReturn(true);

        assertThat(service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1500.00"), "DOP"))).isFalse();

        verify(reviews, never()).saveAndFlush(any(PaymentReconciliationCase.class));
        verifyNoInteractions(transactions);
    }

    @Test
    void missingVerifiedReceiptCannotBeReconciled() {
        assertThatThrownBy(() -> service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1500.00"), "DOP")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificada");
        verifyNoInteractions(reviews, transactions);
    }

    @Test
    void mismatchedReceiptDigestCannotBeReconciled() {
        when(inbox.findByProviderAndExternalEventId(
                PaymentProvider.AZUL, "bank_event_123")).thenReturn(
                        Optional.of(new PaymentWebhookInbox(
                                PaymentProvider.AZUL,
                                "bank_event_123", "0".repeat(64),
                                java.time.LocalDateTime.now())));

        assertThatThrownBy(() -> service.reviewPreviouslyVerified(evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1500.00"), "DOP")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coincide");
        verifyNoInteractions(reviews, transactions);
    }

    @Test
    void evidenceRejectsNegativeAmountsAndMalformedCurrencies() {
        assertThatThrownBy(() -> evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("-1.00"), "DOP"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evidence(
                PaymentStatus.APPROVED, "bank_payment_123",
                new BigDecimal("1500.00"), "DOP;"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
