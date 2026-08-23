package com.jairomatias.eventix.payment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;

import jakarta.persistence.LockModeType;

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

    Optional<PaymentTransaction>
            findFirstBySale_ReferenceCodeAndProviderAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                    String saleReference,
                    PaymentProvider provider,
                    PaymentTransactionType transactionType,
                    PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"sale", "sale.event", "sale.event.organizer"})
    @Query("""
            SELECT transaction
            FROM PaymentTransaction transaction
            WHERE transaction.sale.event.organizer.id = :organizerId
            AND transaction.transactionType = com.jairomatias.eventix.payment.entity.PaymentTransactionType.REFUND
            AND transaction.status = com.jairomatias.eventix.payment.entity.PaymentStatus.APPROVED
            AND transaction.processedAt >= :fromDate
            AND transaction.processedAt < :toDate
            AND NOT EXISTS (
                SELECT line.id
                FROM OrganizerSettlementLine line
                WHERE line.paymentTransaction = transaction
                AND line.active = true
            )
            ORDER BY transaction.processedAt ASC, transaction.id ASC
            """)
    List<PaymentTransaction> findUnsettledRefundsForUpdate(
            @Param("organizerId") Long organizerId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);
}
