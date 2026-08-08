package com.jairomatias.eventix.ticket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.ticket.entity.ScanOutcome;
import com.jairomatias.eventix.ticket.entity.TicketScanAttempt;

public interface TicketScanAttemptRepository
        extends JpaRepository<TicketScanAttempt, Long> {

    @EntityGraph(attributePaths = {
            "ticket", "event", "scannedBy"
    })
    @Query(
            value = """
                    SELECT a
                    FROM TicketScanAttempt a
                    LEFT JOIN a.event e
                    LEFT JOIN e.organizer o
                    WHERE (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """,
            countQuery = """
                    SELECT COUNT(a)
                    FROM TicketScanAttempt a
                    LEFT JOIN a.event e
                    LEFT JOIN e.organizer o
                    WHERE (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """)
    Page<TicketScanAttempt> findVisible(
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(a)
            FROM TicketScanAttempt a
            WHERE a.outcome = :outcome
            AND (:eventId IS NULL OR a.event.id = :eventId)
            AND (:organizerId IS NULL OR a.event.organizer.id = :organizerId)
            """)
    long countVisibleByOutcome(
            @Param("outcome") ScanOutcome outcome,
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId);

    @Query("""
            SELECT COUNT(a)
            FROM TicketScanAttempt a
            WHERE a.firstAccess = true
            AND (:eventId IS NULL OR a.event.id = :eventId)
            AND (:organizerId IS NULL OR a.event.organizer.id = :organizerId)
            """)
    long countVisibleFirstAccesses(
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId);
}
