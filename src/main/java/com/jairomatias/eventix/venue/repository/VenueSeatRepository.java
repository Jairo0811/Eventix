package com.jairomatias.eventix.venue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.venue.entity.VenueSeat;

public interface VenueSeatRepository extends JpaRepository<VenueSeat, Long> {

    List<VenueSeat> findAllByRowIdAndActiveTrueOrderBySeatNumberAsc(Long rowId);

    boolean existsByRowIdAndSeatNumberIgnoreCase(Long rowId, String seatNumber);

    long countByRowSectionVenueIdAndActiveTrue(Long venueId);

    @Query("""
            SELECT s
            FROM VenueSeat s
            JOIN FETCH s.row r
            JOIN FETCH r.section sec
            WHERE sec.venue.id = :venueId
            AND sec.active = true
            AND r.active = true
            AND s.active = true
            AND sec.sectionType = com.jairomatias.eventix.venue.entity.VenueSectionType.RESERVED_SEATING
            ORDER BY sec.sortOrder, r.sortOrder, s.seatNumber
            """)
    List<VenueSeat> findActiveReservedSeatsByVenueId(
            @Param("venueId") Long venueId);
}
