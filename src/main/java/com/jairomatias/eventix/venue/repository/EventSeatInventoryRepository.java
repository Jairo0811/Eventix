package com.jairomatias.eventix.venue.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;

import jakarta.persistence.LockModeType;

public interface EventSeatInventoryRepository
        extends JpaRepository<EventSeatInventory, Long> {

    boolean existsByEvent_Id(Long eventId);

    @EntityGraph(attributePaths = {
            "seat",
            "seat.row",
            "seat.row.section"
    })
    List<EventSeatInventory> findAllByEvent_IdOrderBySeat_Row_Section_SortOrderAscSeat_Row_SortOrderAscSeat_SeatNumberAsc(
            Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM EventSeatInventory i
            JOIN FETCH i.seat s
            JOIN FETCH s.row r
            JOIN FETCH r.section sec
            WHERE i.event.id = :eventId
            AND s.id IN :seatIds
            ORDER BY i.id
            """)
    List<EventSeatInventory> findForUpdate(
            @Param("eventId") Long eventId,
            @Param("seatIds") Collection<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM EventSeatInventory i
            WHERE i.event.id = :eventId
            AND i.holdToken = :holdToken
            ORDER BY i.id
            """)
    List<EventSeatInventory> findHeldByTokenForUpdate(
            @Param("eventId") Long eventId,
            @Param("holdToken") String holdToken);

    long countByEvent_IdAndStatus(Long eventId, EventSeatStatus status);

    @EntityGraph(attributePaths = {
            "seat",
            "seat.row",
            "seat.row.section"
    })
    List<EventSeatInventory> findAllBySale_IdOrderBySeat_Row_Section_SortOrderAscSeat_Row_SortOrderAscSeat_SeatNumberAsc(
            Long saleId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE EventSeatInventory i
            SET i.status = com.jairomatias.eventix.venue.entity.EventSeatStatus.AVAILABLE,
                i.holdToken = null,
                i.holdExpiresAt = null,
                i.updatedAt = :now,
                i.updatedBy = 'seat-hold-expiration',
                i.version = i.version + 1
            WHERE i.status = com.jairomatias.eventix.venue.entity.EventSeatStatus.HELD
            AND i.holdExpiresAt <= :now
            """)
    int releaseExpiredHolds(@Param("now") LocalDateTime now);

    Optional<EventSeatInventory> findByEvent_IdAndSeat_Id(Long eventId, Long seatId);
}
