package com.jairomatias.eventix.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;

import jakarta.persistence.LockModeType;

public interface DigitalTicketRepository
        extends JpaRepository<DigitalTicket, Long> {

    boolean existsBySale_Id(Long saleId);

    boolean existsByUniqueCode(String uniqueCode);

    boolean existsByAntiFraudCode(String antiFraudCode);

    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    Optional<DigitalTicket> findDetailedById(Long id);

    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    Optional<DigitalTicket> findByUniqueCode(String uniqueCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    @Query("SELECT t FROM DigitalTicket t WHERE t.uniqueCode = :uniqueCode")
    Optional<DigitalTicket> findByUniqueCodeForUpdate(
            @Param("uniqueCode") String uniqueCode);

    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    List<DigitalTicket> findAllBySale_IdOrderBySequenceNumberAsc(Long saleId);

    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    List<DigitalTicket> findAllByEvent_IdOrderBySequenceNumberAsc(
            Long eventId);

    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    List<DigitalTicket> findAllByStatusAndEvent_EndAtBefore(
            TicketStatus status,
            LocalDateTime endAt);

    @EntityGraph(attributePaths = {
            "sale", "saleItem", "event", "event.organizer"
    })
    List<DigitalTicket> findAllByEvent_IdAndPassUpdatedAtAfterOrderByPassUpdatedAtAsc(
            Long eventId,
            LocalDateTime updatedAfter);

    @Query(
            value = """
                    SELECT t
                    FROM DigitalTicket t
                    JOIN FETCH t.sale s
                    JOIN FETCH t.saleItem si
                    JOIN FETCH t.event e
                    JOIN FETCH e.organizer o
                    WHERE (
                        :term = '' OR
                        LOWER(t.uniqueCode) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(t.attendeeName) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(t.attendeeEmail) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(s.referenceCode) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.title) LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR t.status = :status)
                    AND (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """,
            countQuery = """
                    SELECT COUNT(t)
                    FROM DigitalTicket t
                    JOIN t.sale s
                    JOIN t.event e
                    JOIN e.organizer o
                    WHERE (
                        :term = '' OR
                        LOWER(t.uniqueCode) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(t.attendeeName) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(t.attendeeEmail) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(s.referenceCode) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.title) LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR t.status = :status)
                    AND (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """)
    Page<DigitalTicket> search(
            @Param("term") String term,
            @Param("status") TicketStatus status,
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(t)
            FROM DigitalTicket t
            WHERE (:eventId IS NULL OR t.event.id = :eventId)
            AND (:organizerId IS NULL OR t.event.organizer.id = :organizerId)
            """)
    long countVisible(
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId);

    @Query("""
            SELECT COUNT(t)
            FROM DigitalTicket t
            WHERE t.status = :status
            AND (:eventId IS NULL OR t.event.id = :eventId)
            AND (:organizerId IS NULL OR t.event.organizer.id = :organizerId)
            """)
    long countVisibleByStatus(
            @Param("status") TicketStatus status,
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId);
}
