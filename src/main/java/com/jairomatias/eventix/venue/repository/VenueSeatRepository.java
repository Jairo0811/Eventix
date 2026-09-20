package com.jairomatias.eventix.venue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.venue.entity.VenueSeat;

public interface VenueSeatRepository extends JpaRepository<VenueSeat, Long> {

    List<VenueSeat> findAllByRowIdAndActiveTrueOrderBySeatNumberAsc(Long rowId);

    boolean existsByRowIdAndSeatNumberIgnoreCase(Long rowId, String seatNumber);

    long countByRowSectionVenueIdAndActiveTrue(Long venueId);
}
