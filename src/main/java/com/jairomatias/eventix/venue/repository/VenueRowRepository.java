package com.jairomatias.eventix.venue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.venue.entity.VenueRow;

public interface VenueRowRepository extends JpaRepository<VenueRow, Long> {

    List<VenueRow> findAllBySectionIdAndActiveTrueOrderBySortOrderAscCodeAsc(Long sectionId);

    boolean existsBySectionIdAndCodeIgnoreCase(Long sectionId, String code);
}
