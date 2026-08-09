package com.jairomatias.eventix.reporting.service;

import java.util.List;

import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.dto.ReportOption;

public interface ReportService {

    ReportDataset generate(ReportFilter filter, Long forcedOrganizerId);

    List<ReportOption> findEvents(Long forcedOrganizerId);

    List<ReportOption> findCategories();

    List<ReportOption> findOrganizers();
}
