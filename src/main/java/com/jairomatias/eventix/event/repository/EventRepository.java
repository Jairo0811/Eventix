package com.jairomatias.eventix.event.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;

import jakarta.persistence.LockModeType;

public interface EventRepository extends JpaRepository<Event, Long> {

    long countByStatus(EventStatus status);

    @EntityGraph(attributePaths = {"category", "organizer"})
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "organizer"})
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findDetailedByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "organizer"})
    List<Event> findAllByStatusAndStartAtAfterOrderByStartAtAsc(
            EventStatus status,
            LocalDateTime startAt);

    @EntityGraph(attributePaths = {"category", "organizer"})
    List<Event> findAllByOrganizer_IdOrderByStartAtDesc(Long organizerId);

    @Query(
            value = """
                    SELECT e
                    FROM Event e
                    JOIN FETCH e.category c
                    JOIN FETCH e.organizer o
                    WHERE (
                        :term = '' OR
                        LOWER(e.title)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.venue)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(o.firstName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(o.lastName)
                            LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR e.status = :status)
                    AND (:categoryId IS NULL OR c.id = :categoryId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    AND e.status IN :visibleStatuses
                    """,
            countQuery = """
                    SELECT COUNT(e)
                    FROM Event e
                    JOIN e.category c
                    JOIN e.organizer o
                    WHERE (
                        :term = '' OR
                        LOWER(e.title)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(e.venue)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(o.firstName)
                            LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(o.lastName)
                            LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR e.status = :status)
                    AND (:categoryId IS NULL OR c.id = :categoryId)
                    AND (:organizerId IS NULL OR o.id = :organizerId)
                    AND e.status IN :visibleStatuses
                    """)
    Page<Event> search(
            @Param("term") String term,
            @Param("status") EventStatus status,
            @Param("categoryId") Long categoryId,
            @Param("organizerId") Long organizerId,
            @Param("visibleStatuses")
            Collection<EventStatus> visibleStatuses,
            Pageable pageable);
}
