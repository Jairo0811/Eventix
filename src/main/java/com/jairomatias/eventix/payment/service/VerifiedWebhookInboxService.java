package com.jairomatias.eventix.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentWebhookInbox;
import com.jairomatias.eventix.payment.repository.PaymentWebhookInboxRepository;

/**
 * Persistence only — NOT an HTTP endpoint or a webhook signature verifier.
 * Call only after authenticating the notification with the provider's official
 * documented signature verification. It does not mark sales paid or issue QR.
 * A unique database constraint also blocks concurrent duplicate insertions.
 */
@Service
public class VerifiedWebhookInboxService {

    private static final Pattern SHA256 = Pattern.compile("[a-fA-F0-9]{64}");

    private final PaymentWebhookInboxRepository repository;
    private final Clock clock;

    public VerifiedWebhookInboxService(
            PaymentWebhookInboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public boolean recordVerifiedDelivery(
            PaymentProvider provider,
            String externalEventId,
            String payloadSha256) {
        if (provider == null
                || !StringUtils.hasText(externalEventId)
                || externalEventId.length() > 160
                || payloadSha256 == null
                || !SHA256.matcher(payloadSha256).matches()) {
            throw new IllegalArgumentException(
                    "Identificadores de notificación verificada inválidos.");
        }

        if (repository.existsByProviderAndExternalEventId(
                provider, externalEventId)) {
            return false;
        }

        repository.saveAndFlush(new PaymentWebhookInbox(
                provider,
                externalEventId,
                payloadSha256.toLowerCase(Locale.ROOT),
                LocalDateTime.now(clock)));
        return true;
    }
}
