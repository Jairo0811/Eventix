package com.jairomatias.eventix.venue.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.venue.dto.VenueLayoutView;
import com.jairomatias.eventix.venue.dto.VenueRowForm;
import com.jairomatias.eventix.venue.dto.VenueSeatForm;
import com.jairomatias.eventix.venue.dto.VenueSectionForm;
import com.jairomatias.eventix.venue.entity.Venue;
import com.jairomatias.eventix.venue.entity.VenueRow;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.entity.VenueSection;
import com.jairomatias.eventix.venue.repository.VenueRepository;
import com.jairomatias.eventix.venue.repository.VenueRowRepository;
import com.jairomatias.eventix.venue.repository.VenueSeatRepository;
import com.jairomatias.eventix.venue.repository.VenueSectionRepository;

@Service
public class DefaultVenueLayoutService implements VenueLayoutService {

    private static final BigDecimal MIN_POSITION = BigDecimal.ZERO;
    private static final BigDecimal MAX_POSITION = BigDecimal.valueOf(100);

    private final VenueRepository venueRepository;
    private final VenueSectionRepository sectionRepository;
    private final VenueRowRepository rowRepository;
    private final VenueSeatRepository seatRepository;

    public DefaultVenueLayoutService(
            VenueRepository venueRepository,
            VenueSectionRepository sectionRepository,
            VenueRowRepository rowRepository,
            VenueSeatRepository seatRepository) {
        this.venueRepository = venueRepository;
        this.sectionRepository = sectionRepository;
        this.rowRepository = rowRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public VenueLayoutView getLayout(Long venueId) {
        Venue venue = findVenue(venueId);

        List<VenueLayoutView.Section> sections = sectionRepository
                .findAllByVenueIdAndActiveTrueOrderBySortOrderAscNameAsc(venueId)
                .stream()
                .map(section -> new VenueLayoutView.Section(
                        section.getId(),
                        section.getCode(),
                        section.getName(),
                        section.getSectionType(),
                        section.getCapacity(),
                        rowRepository
                                .findAllBySectionIdAndActiveTrueOrderBySortOrderAscCodeAsc(section.getId())
                                .stream()
                                .map(row -> new VenueLayoutView.Row(
                                        row.getId(),
                                        row.getCode(),
                                        row.getLabel(),
                                        seatRepository
                                                .findAllByRowIdAndActiveTrueOrderBySeatNumberAsc(row.getId())
                                                .stream()
                                                .map(seat -> new VenueLayoutView.Seat(
                                                        seat.getId(),
                                                        seat.getSeatNumber(),
                                                        seat.getLabel(),
                                                        seat.isAccessible(),
                                                        seat.isCompanionSeat(),
                                                        seat.getXPosition(),
                                                        seat.getYPosition()))
                                                .toList()))
                                .toList()))
                .toList();

        return new VenueLayoutView(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getCountryCode(),
                sections);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long addSection(Long venueId, VenueSectionForm form) {
        Venue venue = findVenue(venueId);
        String code = form.getCode().trim().toUpperCase();

        if (sectionRepository.existsByVenueIdAndCodeIgnoreCase(venueId, code)) {
            throw new DuplicateResourceException(
                    "code",
                    "Ya existe una sección con ese código en el recinto.");
        }

        VenueSection section = new VenueSection(
                venue,
                code,
                form.getName().trim(),
                form.getSectionType(),
                form.getCapacity(),
                form.getSortOrder());

        return sectionRepository.save(section).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long addRow(Long venueId, Long sectionId, VenueRowForm form) {
        VenueSection section = findSectionInVenue(venueId, sectionId);
        String code = form.getCode().trim().toUpperCase();

        if (rowRepository.existsBySectionIdAndCodeIgnoreCase(sectionId, code)) {
            throw new DuplicateResourceException(
                    "code",
                    "Ya existe una fila con ese código en la sección.");
        }

        VenueRow row = new VenueRow(
                section,
                code,
                form.getLabel().trim(),
                form.getSortOrder());

        return rowRepository.save(row).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long addSeat(Long venueId, Long sectionId, Long rowId, VenueSeatForm form) {
        VenueRow row = findRowInSection(venueId, sectionId, rowId);
        String seatNumber = form.getSeatNumber().trim().toUpperCase();

        if (seatRepository.existsByRowIdAndSeatNumberIgnoreCase(rowId, seatNumber)) {
            throw new DuplicateResourceException(
                    "seatNumber",
                    "Ya existe ese número de asiento en la fila.");
        }

        validateCoordinates(form.getXPosition(), form.getYPosition());
        VenueSeat seat = new VenueSeat(
                row,
                seatNumber,
                form.getLabel().trim(),
                form.isAccessible(),
                form.isCompanionSeat());
        seat.update(
                seatNumber,
                form.getLabel().trim(),
                form.isAccessible(),
                form.isCompanionSeat(),
                normalizeCoordinate(form.getXPosition()),
                normalizeCoordinate(form.getYPosition()),
                true);

        return seatRepository.save(seat).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void updateSeatPosition(
            Long venueId,
            Long sectionId,
            Long rowId,
            Long seatId,
            BigDecimal xPosition,
            BigDecimal yPosition) {
        VenueSeat seat = findSeatInRow(venueId, sectionId, rowId, seatId);
        validateCoordinates(xPosition, yPosition);
        seat.update(
                seat.getSeatNumber(),
                seat.getLabel(),
                seat.isAccessible(),
                seat.isCompanionSeat(),
                normalizeCoordinate(xPosition),
                normalizeCoordinate(yPosition),
                seat.isActive());
        seatRepository.save(seat);
    }

    private Venue findVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El recinto solicitado no existe."));
    }

    private VenueSection findSectionInVenue(Long venueId, Long sectionId) {
        VenueSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La sección solicitada no existe."));

        if (!section.getVenue().getId().equals(venueId)) {
            throw new ResourceNotFoundException(
                    "La sección no pertenece al recinto indicado.");
        }
        return section;
    }

    private VenueRow findRowInSection(Long venueId, Long sectionId, Long rowId) {
        VenueSection section = findSectionInVenue(venueId, sectionId);
        VenueRow row = rowRepository.findById(rowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La fila solicitada no existe."));

        if (!row.getSection().getId().equals(section.getId())) {
            throw new ResourceNotFoundException(
                    "La fila no pertenece a la sección indicada.");
        }
        return row;
    }

    private VenueSeat findSeatInRow(
            Long venueId,
            Long sectionId,
            Long rowId,
            Long seatId) {
        VenueRow row = findRowInSection(venueId, sectionId, rowId);
        VenueSeat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El asiento solicitado no existe."));
        if (!seat.getRow().getId().equals(row.getId())) {
            throw new ResourceNotFoundException(
                    "El asiento no pertenece a la fila indicada.");
        }
        return seat;
    }

    private void validateCoordinates(BigDecimal xPosition, BigDecimal yPosition) {
        if ((xPosition == null) != (yPosition == null)) {
            throw new BusinessRuleException(
                    "Indica ambas coordenadas X e Y, o deja ambas vacías.");
        }
        if (xPosition == null) {
            return;
        }
        if (xPosition.compareTo(MIN_POSITION) < 0
                || xPosition.compareTo(MAX_POSITION) > 0
                || yPosition.compareTo(MIN_POSITION) < 0
                || yPosition.compareTo(MAX_POSITION) > 0) {
            throw new BusinessRuleException(
                    "Las coordenadas del asiento deben estar entre 0 y 100.");
        }
    }

    private BigDecimal normalizeCoordinate(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }
}
