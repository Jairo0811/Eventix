package com.jairomatias.eventix.reporting.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

import com.jairomatias.eventix.reporting.dto.ReportSummary;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.repository.NormalizedReportFilter;
import com.jairomatias.eventix.reporting.repository.ReportingRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

class DefaultReportServiceTest {

    private final ReportingRepository repository = mock(ReportingRepository.class);
    private final DefaultReportService service = new DefaultReportService(repository);

    @Test
    void organizerScopeOverridesSubmittedOrganizer() {
        ReportFilter filter = new ReportFilter();
        filter.setFrom(LocalDate.of(2026, 8, 1));
        filter.setTo(LocalDate.of(2026, 8, 31));
        filter.setOrganizerId(99L);
        when(repository.generate(any())).thenReturn(new ReportDataset(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new ReportSummary(
                        BigDecimal.ZERO, 0, 0, 0, 0, 0,
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of()));

        service.generate(filter, 7L);

        ArgumentCaptor<NormalizedReportFilter> captor =
                ArgumentCaptor.forClass(NormalizedReportFilter.class);
        verify(repository).generate(captor.capture());
        org.assertj.core.api.Assertions.assertThat(
                captor.getValue().organizerId()).isEqualTo(7L);
    }

    @Test
    void rejectsInvertedDateRange() {
        ReportFilter filter = new ReportFilter();
        filter.setFrom(LocalDate.of(2026, 9, 1));
        filter.setTo(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> service.generate(filter, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fecha inicial");
    }

    @Test
    void rejectsRangesLongerThanFiveYears() {
        ReportFilter filter = new ReportFilter();
        filter.setFrom(LocalDate.of(2020, 1, 1));
        filter.setTo(LocalDate.of(2026, 1, 2));

        assertThatThrownBy(() -> service.generate(filter, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cinco años");
    }
}
