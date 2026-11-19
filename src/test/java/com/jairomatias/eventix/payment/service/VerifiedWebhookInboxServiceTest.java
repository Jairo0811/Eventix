package com.jairomatias.eventix.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentWebhookInbox;
import com.jairomatias.eventix.payment.repository.PaymentWebhookInboxRepository;

@ExtendWith(MockitoExtension.class)
class VerifiedWebhookInboxServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-19T12:00:00Z"),
            ZoneId.of("America/Santo_Domingo"));

    @Mock private PaymentWebhookInboxRepository repository;

    @Test
    void persistsOnlyMinimalVerifiedIdentifiersAndDigest() {
        String digest = "ABCDEF1234567890".repeat(4);
        VerifiedWebhookInboxService service = new VerifiedWebhookInboxService(repository, CLOCK);

        assertThat(service.recordVerifiedDelivery(
                PaymentProvider.AZUL, "event_1234", digest)).isTrue();

        ArgumentCaptor<PaymentWebhookInbox> captor =
                ArgumentCaptor.forClass(PaymentWebhookInbox.class);
        verify(repository).saveAndFlush(captor.capture());
        PaymentWebhookInbox event = captor.getValue();
        assertThat(event.getProvider()).isEqualTo(PaymentProvider.AZUL);
        assertThat(event.getExternalEventId()).isEqualTo("event_1234");
        assertThat(event.getPayloadSha256()).isEqualTo(digest.toLowerCase());
        assertThat(event.getStatus()).isEqualTo("VERIFIED");
        assertThat(event.getProcessedAt()).isNull();
    }

    @Test
    void replayDoesNotCreateAnotherRecord() {
        when(repository.existsByProviderAndExternalEventId(
                PaymentProvider.AZUL, "event_1234")).thenReturn(true);
        VerifiedWebhookInboxService service = new VerifiedWebhookInboxService(repository, CLOCK);

        assertThat(service.recordVerifiedDelivery(
                PaymentProvider.AZUL,
                "event_1234",
                "abcdef1234567890".repeat(4))).isFalse();

        verify(repository, never()).saveAndFlush(any(PaymentWebhookInbox.class));
    }

    @Test
    void malformedEventAndDigestAreRejectedBeforeDatabaseAccess() {
        VerifiedWebhookInboxService service = new VerifiedWebhookInboxService(repository, CLOCK);
        assertThatThrownBy(() -> service.recordVerifiedDelivery(
                PaymentProvider.AZUL, "", "0".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recordVerifiedDelivery(
                PaymentProvider.AZUL, "event_1234", "not-a-sha256"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAndFlush(any(PaymentWebhookInbox.class));
    }
}
