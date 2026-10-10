package com.jairomatias.eventix.venue.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.venue.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    boolean existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(
            String name,
            String city,
            Long id);

    List<Venue> findAllByActiveTrueOrderByNameAsc();

    @Query("""
            SELECT v
            FROM Venue v
            WHERE (
                :term = '' OR
                LOWER(v.name) LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(v.city) LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(v.address) LIKE LOWER(CONCAT('%', :term, '%'))
            )
            AND (:active IS NULL OR v.active = :active)
            """)
    Page<Venue> search(
            @Param("term") String term,
            @Param("active") Boolean active,
            Pageable pageable);
}
