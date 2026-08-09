package com.jairomatias.eventix.reporting.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.dto.ReportOption;
import com.jairomatias.eventix.reporting.repository.NormalizedReportFilter;
import com.jairomatias.eventix.reporting.repository.ReportingRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Service
public class DefaultReportService implements ReportService {

    private static final long MAXIMUM_RANGE_DAYS = 366L * 5L;

    private final ReportingRepository reportingRepository;

    public DefaultReportService(ReportingRepository reportingRepository) {
        this.reportingRepository = reportingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public ReportDataset generate(
            ReportFilter filter,
            Long forcedOrganizerId) {
        return reportingRepository.generate(normalize(filter, forcedOrganizerId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<ReportOption> findEvents(Long forcedOrganizerId) {
        return reportingRepository.findEvents(forcedOrganizerId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public List<ReportOption> findCategories() {
        return reportingRepository.findCategories();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<ReportOption> findOrganizers() {
        return reportingRepository.findOrganizers();
    }

    private NormalizedReportFilter normalize(
            ReportFilter filter,
            Long forcedOrganizerId) {
        LocalDate today = LocalDate.now();
        LocalDate from = filter.getFrom() == null
                ? today.withDayOfYear(1)
                : filter.getFrom();
        LocalDate to = filter.getTo() == null ? today : filter.getTo();

        if (from.isAfter(to)) {
            throw new BusinessRuleException(
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAXIMUM_RANGE_DAYS) {
            throw new BusinessRuleException(
                    "El rango del reporte no puede superar cinco años.");
        }
        Long organizerId = forcedOrganizerId == null
                ? filter.getOrganizerId()
                : forcedOrganizerId;
        return new NormalizedReportFilter(
                from,
                to,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                filter.getEventId(),
                filter.getCategoryId(),
                organizerId);
    }
}
