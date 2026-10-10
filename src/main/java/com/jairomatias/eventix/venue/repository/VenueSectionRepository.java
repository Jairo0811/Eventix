package com.jairomatias.eventix.venue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.venue.entity.VenueSection;

public interface VenueSectionRepository extends JpaRepository<VenueSection, Long> {

    List<VenueSection> findAllByVenueIdAndActiveTrueOrderBySortOrderAscNameAsc(Long venueId);

    boolean existsByVenueIdAndCodeIgnoreCase(Long venueId, String code);
}
