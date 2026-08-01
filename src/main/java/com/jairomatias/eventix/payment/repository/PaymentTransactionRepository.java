package com.jairomatias.eventix.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByTransactionReference(String transactionReference);

    @EntityGraph(attributePaths = {"processedBy"})
    List<PaymentTransaction> findAllBySale_IdOrderByProcessedAtDesc(
            Long saleId);

    Optional<PaymentTransaction>
            findFirstBySale_IdAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                    Long saleId,
                    PaymentTransactionType transactionType,
                    PaymentStatus status);
}
