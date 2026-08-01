package com.jairomatias.eventix.sale.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.sale.entity.TicketType;

import jakarta.persistence.LockModeType;

public interface TicketTypeRepository
        extends JpaRepository<TicketType, Long> {

    boolean existsByEvent_IdAndNameIgnoreCase(Long eventId, String name);

    boolean existsByEvent_IdAndNameIgnoreCaseAndIdNot(
            Long eventId,
            String name,
            Long excludedId);

    @EntityGraph(attributePaths = {"event", "event.organizer"})
    List<TicketType> findAllByEvent_IdOrderByNameAsc(Long eventId);

    @EntityGraph(attributePaths = {"event", "event.organizer"})
    List<TicketType> findAllByEvent_IdAndActiveTrueOrderByNameAsc(
            Long eventId);

    @EntityGraph(attributePaths = {"event", "event.organizer"})
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"event", "event.organizer"})
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findDetailedByIdForUpdate(@Param("id") Long id);
}
