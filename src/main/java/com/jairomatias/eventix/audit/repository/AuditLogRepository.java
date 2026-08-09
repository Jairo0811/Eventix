package com.jairomatias.eventix.audit.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditLog;
import com.jairomatias.eventix.audit.entity.AuditOutcome;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    long countByEventType(AuditEventType eventType);

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE (
                :term = '' OR
                LOWER(COALESCE(a.actorUsername, ''))
                    LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(a.action) LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(COALESCE(a.entityType, ''))
                    LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(a.correlationId) LIKE LOWER(CONCAT('%', :term, '%'))
            )
            AND (:eventType IS NULL OR a.eventType = :eventType)
            AND (:outcome IS NULL OR a.outcome = :outcome)
            AND (:fromDate IS NULL OR a.occurredAt >= :fromDate)
            AND (:toDate IS NULL OR a.occurredAt < :toDate)
            """)
    Page<AuditLog> search(
            @Param("term") String term,
            @Param("eventType") AuditEventType eventType,
            @Param("outcome") AuditOutcome outcome,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
