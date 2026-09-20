package com.jairomatias.eventix.venue.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.venue.dto.VenueForm;
import com.jairomatias.eventix.venue.dto.VenueListItem;

public interface VenueService {

    Page<VenueListItem> findAll(String term, Boolean active, Pageable pageable);

    VenueForm getForm(Long id);

    Long create(VenueForm form);

    void update(Long id, VenueForm form);

    void activate(Long id);

    void deactivate(Long id);
}
