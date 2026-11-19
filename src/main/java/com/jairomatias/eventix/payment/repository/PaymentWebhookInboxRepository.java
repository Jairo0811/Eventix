package com.jairomatias.eventix.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentWebhookInbox;

public interface PaymentWebhookInboxRepository
        extends JpaRepository<PaymentWebhookInbox, Long> {

    Optional<PaymentWebhookInbox> findByProviderAndExternalEventId(
            PaymentProvider provider, String externalEventId);

    boolean existsByProviderAndExternalEventId(
            PaymentProvider provider,
            String externalEventId);
}
