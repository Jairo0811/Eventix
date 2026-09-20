package com.jairomatias.eventix.venue.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.venue.dto.VenueForm;
import com.jairomatias.eventix.venue.dto.VenueListItem;
import com.jairomatias.eventix.venue.entity.Venue;
import com.jairomatias.eventix.venue.repository.VenueRepository;
import com.jairomatias.eventix.venue.repository.VenueSeatRepository;

@Service
public class DefaultVenueService implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueSeatRepository venueSeatRepository;

    public DefaultVenueService(
            VenueRepository venueRepository,
            VenueSeatRepository venueSeatRepository) {
        this.venueRepository = venueRepository;
        this.venueSeatRepository = venueSeatRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<VenueListItem> findAll(String term, Boolean active, Pageable pageable) {
        String normalizedTerm = term == null ? "" : term.trim();
        return venueRepository.search(normalizedTerm, active, pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public VenueForm getForm(Long id) {
        Venue venue = findEntity(id);
        VenueForm form = new VenueForm();
        form.setName(venue.getName());
        form.setAddress(venue.getAddress());
        form.setCity(venue.getCity());
        form.setCountryCode(venue.getCountryCode());
        form.setTimeZone(venue.getTimeZone());
        form.setActive(venue.isActive());
        return form;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long create(VenueForm form) {
        String name = form.getName().trim();
        String city = form.getCity().trim();
        validateUnique(name, city, null);

        Venue venue = new Venue(
                name,
                form.getAddress().trim(),
                city,
                form.getCountryCode().trim().toUpperCase(),
                form.getTimeZone().trim());

        venue.update(
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getCountryCode(),
                venue.getTimeZone(),
                form.isActive());

        return venueRepository.save(venue).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void update(Long id, VenueForm form) {
        Venue venue = findEntity(id);
        String name = form.getName().trim();
        String city = form.getCity().trim();
        validateUnique(name, city, id);

        venue.update(
                name,
                form.getAddress().trim(),
                city,
                form.getCountryCode().trim().toUpperCase(),
                form.getTimeZone().trim(),
                form.isActive());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void activate(Long id) {
        Venue venue = findEntity(id);
        venue.update(
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getCountryCode(),
                venue.getTimeZone(),
                true);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(Long id) {
        Venue venue = findEntity(id);
        venue.update(
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getCountryCode(),
                venue.getTimeZone(),
                false);
    }

    private Venue findEntity(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El recinto solicitado no existe."));
    }

    private void validateUnique(String name, String city, Long excludedId) {
        boolean duplicated = excludedId == null
                ? venueRepository.existsByNameIgnoreCaseAndCityIgnoreCase(name, city)
                : venueRepository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(name, city, excludedId);

        if (duplicated) {
            throw new DuplicateResourceException(
                    "name",
                    "Ya existe un recinto con ese nombre en la misma ciudad.");
        }
    }

    private VenueListItem toListItem(Venue venue) {
        return new VenueListItem(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getCountryCode(),
                venue.getTimeZone(),
                venue.isActive(),
                venueSeatRepository.countByRowSectionVenueIdAndActiveTrue(venue.getId()));
    }
}
