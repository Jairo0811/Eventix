package com.jairomatias.eventix.settlement.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.settlement.entity.OrganizerSettlement;
import com.jairomatias.eventix.settlement.entity.SettlementStatus;

import jakarta.persistence.LockModeType;

public interface OrganizerSettlementRepository
        extends JpaRepository<OrganizerSettlement, Long> {

    @EntityGraph(attributePaths = "organizer")
    @Query(
            value = """
                    SELECT st
                    FROM OrganizerSettlement st
                    JOIN st.organizer o
                    WHERE (:organizerId IS NULL OR o.id = :organizerId)
                    AND (:status IS NULL OR st.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(st)
                    FROM OrganizerSettlement st
                    WHERE (:organizerId IS NULL
                           OR st.organizer.id = :organizerId)
                    AND (:status IS NULL OR st.status = :status)
                    """)
    Page<OrganizerSettlement> search(
            @Param("organizerId") Long organizerId,
            @Param("status") SettlementStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "organizer",
            "lines",
            "lines.sale",
            "lines.sale.event"
    })
    @Query("SELECT st FROM OrganizerSettlement st WHERE st.id = :id")
    Optional<OrganizerSettlement> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"organizer", "lines"})
    @Query("SELECT st FROM OrganizerSettlement st WHERE st.id = :id")
    Optional<OrganizerSettlement> findDetailedByIdForUpdate(
            @Param("id") Long id);

    long countByOrganizer_IdAndStatusIn(
            Long organizerId,
            Collection<SettlementStatus> statuses);

    long countByOrganizer_IdAndStatus(
            Long organizerId,
            SettlementStatus status);
}
