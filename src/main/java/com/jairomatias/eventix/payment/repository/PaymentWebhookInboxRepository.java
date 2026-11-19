package com.jairomatias.eventix.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentWebhookInbox;

public interface PaymentWebhookInboxRepository
        extends JpaRepository<PaymentWebhookInbox, Long> {

    boolean existsByProviderAndExternalEventId(
            PaymentProvider provider,
            String externalEventId);
}
