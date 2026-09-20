package com.jairomatias.eventix.venue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.venue.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    List<Venue> findAllByActiveTrueOrderByNameAsc();
}
