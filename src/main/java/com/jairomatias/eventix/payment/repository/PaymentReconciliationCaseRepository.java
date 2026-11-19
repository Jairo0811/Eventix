package com.jairomatias.eventix.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentReconciliationCase;

public interface PaymentReconciliationCaseRepository
        extends JpaRepository<PaymentReconciliationCase, Long> {

    boolean existsByProviderAndExternalEventId(
            PaymentProvider provider, String externalEventId);

    List<PaymentReconciliationCase> findAllByOutcomeOrderByReviewedAtAsc(
            String outcome);
}
