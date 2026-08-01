package com.jairomatias.eventix.sale.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;

import jakarta.persistence.LockModeType;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    long countByStatus(SaleStatus status);

    boolean existsByReferenceCode(String referenceCode);

    boolean existsByReservation_Id(Long reservationId);

    @Query("SELECT s.event.id FROM Sale s WHERE s.id = :id")
    Optional<Long> findEventIdById(@Param("id") Long id);

    @Query("SELECT s.reservation.id FROM Sale s WHERE s.id = :id")
    Optional<Long> findReservationIdById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "reservation",
            "event",
            "event.organizer",
            "soldBy",
            "items",
            "items.ticketType"
    })
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "reservation",
            "event",
            "event.organizer",
            "soldBy",
            "items",
            "items.ticketType"
    })
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findDetailedByIdForUpdate(@Param("id") Long id);

    @Query(
            value = """
                    SELECT s
                    FROM Sale s
                    JOIN FETCH s.reservation r
                    JOIN FETCH s.event e
                    JOIN FETCH e.organizer o
                    JOIN FETCH s.soldBy u
                    WHERE (
                        :term = '' OR
                        LOWER(s.referenceCode)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(s.buyerName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(s.buyerEmail)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.referenceCode)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.title)
                            LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR s.status = :status)
                    AND (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """,
            countQuery = """
                    SELECT COUNT(s)
                    FROM Sale s
                    JOIN s.reservation r
                    JOIN s.event e
                    JOIN e.organizer o
                    WHERE (
                        :term = '' OR
                        LOWER(s.referenceCode)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(s.buyerName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(s.buyerEmail)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.referenceCode)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.title)
                            LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR s.status = :status)
                    AND (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """)
    Page<Sale> search(
            @Param("term") String term,
            @Param("status") SaleStatus status,
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(s.total), 0)
            FROM Sale s
            WHERE s.status = :status
            AND (:organizerId IS NULL OR s.event.organizer.id = :organizerId)
            """)
    BigDecimal sumTotalByStatus(
            @Param("status") SaleStatus status,
            @Param("organizerId") Long organizerId);

    @Query("""
            SELECT COUNT(s)
            FROM Sale s
            WHERE s.status = :status
            AND (:organizerId IS NULL OR s.event.organizer.id = :organizerId)
            """)
    long countByStatusAndOrganizer(
            @Param("status") SaleStatus status,
            @Param("organizerId") Long organizerId);

    @Query("""
            SELECT COUNT(s)
            FROM Sale s
            WHERE :organizerId IS NULL OR s.event.organizer.id = :organizerId
            """)
    long countByOrganizer(@Param("organizerId") Long organizerId);
}
