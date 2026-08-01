package com.jairomatias.eventix.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;

import jakarta.persistence.LockModeType;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    long countByStatus(ReservationStatus status);

    boolean existsByReferenceCode(String referenceCode);

    boolean existsByEvent_Id(Long eventId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.event.id = :eventId
            AND (
                r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.CONFIRMED
                OR (
                    r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
                    AND r.expiresAt > :now
                )
            )
            """)
    boolean existsActiveByEvent(
            @Param("eventId") Long eventId,
            @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {
            "event",
            "event.organizer",
            "reservedBy"
    })
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findDetailedById(@Param("id") Long id);

    @Query("SELECT r.event.id FROM Reservation r WHERE r.id = :id")
    Optional<Long> findEventIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "event",
            "event.organizer",
            "reservedBy"
    })
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findDetailedByIdForUpdate(
            @Param("id") Long id);

    @EntityGraph(attributePaths = {"event"})
    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.CONFIRMED
            AND r.event.startAt > :now
            AND NOT EXISTS (
                SELECT s.id
                FROM Sale s
                WHERE s.reservation.id = r.id
            )
            ORDER BY r.createdAt ASC
            """)
    List<Reservation> findConfirmedWithoutSale(
            @Param("now") LocalDateTime now);

    @Query(
            value = """
                    SELECT r
                    FROM Reservation r
                    JOIN FETCH r.event e
                    JOIN FETCH e.organizer o
                    JOIN FETCH r.reservedBy u
                    WHERE (
                        :term = '' OR
                        LOWER(r.referenceCode)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.attendeeFirstName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.attendeeLastName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.attendeeEmail)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.title)
                            LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR r.status = :status)
                    AND (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM Reservation r
                    JOIN r.event e
                    JOIN e.organizer o
                    WHERE (
                        :term = '' OR
                        LOWER(r.referenceCode)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.attendeeFirstName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.attendeeLastName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(r.attendeeEmail)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.title)
                            LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR r.status = :status)
                    AND (:eventId IS NULL OR e.id = :eventId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    """)
    Page<Reservation> search(
            @Param("term") String term,
            @Param("status") ReservationStatus status,
            @Param("eventId") Long eventId,
            @Param("organizerId") Long organizerId,
            Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.event.id = :eventId
            AND LOWER(r.attendeeEmail) = LOWER(:attendeeEmail)
            AND (
                r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.CONFIRMED
                OR (
                    r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
                    AND r.expiresAt > :now
                )
            )
            AND (:excludedId IS NULL OR r.id <> :excludedId)
            """)
    boolean existsActiveDuplicate(
            @Param("eventId") Long eventId,
            @Param("attendeeEmail") String attendeeEmail,
            @Param("now") LocalDateTime now,
            @Param("excludedId") Long excludedId);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM Reservation r
            WHERE r.event.id = :eventId
            AND (
                r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.CONFIRMED
                OR (
                    r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
                    AND r.expiresAt > :now
                )
            )
            """)
    long sumOccupiedSeats(
            @Param("eventId") Long eventId,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM Reservation r
            WHERE r.event.id = :eventId
            AND r.id <> :excludedId
            AND (
                r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.CONFIRMED
                OR (
                    r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
                    AND r.expiresAt > :now
                )
            )
            """)
    long sumOccupiedSeatsExcluding(
            @Param("eventId") Long eventId,
            @Param("excludedId") Long excludedId,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM Reservation r
            WHERE r.event.id = :eventId
            AND r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
            AND r.expiresAt > :now
            """)
    long sumPendingSeats(
            @Param("eventId") Long eventId,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM Reservation r
            WHERE r.event.id = :eventId
            AND r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.CONFIRMED
            """)
    long sumConfirmedSeats(@Param("eventId") Long eventId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Reservation r
            SET r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.EXPIRED,
                r.updatedAt = :now,
                r.updatedBy = 'reservation-expiration',
                r.version = r.version + 1
            WHERE r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
            AND r.expiresAt <= :now
            """)
    int expirePendingBefore(@Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Reservation r
            SET r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.EXPIRED,
                r.updatedAt = :now,
                r.updatedBy = 'reservation-expiration',
                r.version = r.version + 1
            WHERE r.event.id = :eventId
            AND r.status = com.jairomatias.eventix.reservation.entity.ReservationStatus.PENDING
            AND r.expiresAt <= :now
            """)
    int expirePendingForEvent(
            @Param("eventId") Long eventId,
            @Param("now") LocalDateTime now);
}
