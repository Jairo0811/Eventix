package com.jairomatias.eventix.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.dashboard.dto.OrganizerDashboardMetrics;
import com.jairomatias.eventix.dashboard.repository.OrganizerDashboardRepository;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.service.ReportService;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrganizerDashboardServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 8, 10, 12, 0);

    @Mock private OrganizerDashboardRepository dashboardRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReportService reportService;
    @Mock private User organizer;
    @Mock private Role organizerRole;

    private OrganizerDashboardService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T12:00:00Z"),
                ZoneOffset.UTC);
        service = new OrganizerDashboardService(
                dashboardRepository,
                userRepository,
                reportService,
                clock);
    }

    @Test
    void scopesCommercialMetricsAndHistoryToAuthenticatedOrganizer() {
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "organizer@example.com",
                "organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(organizer.getId()).thenReturn(8L);
        when(organizer.getRole()).thenReturn(organizerRole);
        when(organizerRole.getName()).thenReturn(RoleName.ORGANIZER);
        when(dashboardRepository.loadMetrics(8L, NOW))
                .thenReturn(metrics());
        when(dashboardRepository.loadUpcomingEvents(8L, NOW))
                .thenReturn(List.of());
        when(reportService.generate(any(ReportFilter.class), eq(8L)))
                .thenReturn(new ReportDataset(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 8, 10),
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        var summary = service.getSummary("organizer@example.com");

        assertThat(summary.occupancyRate())
                .isEqualByComparingTo("25.00");
        assertThat(summary.grossSales())
                .isEqualByComparingTo("100000.00");
        verify(dashboardRepository).loadMetrics(8L, NOW);
        verify(dashboardRepository).loadUpcomingEvents(8L, NOW);
        verify(reportService).generate(any(ReportFilter.class), eq(8L));
    }

    private OrganizerDashboardMetrics metrics() {
        return new OrganizerDashboardMetrics(
                2,
                4,
                1000,
                250,
                35,
                new BigDecimal("100000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("4600.00"),
                new BigDecimal("87400.00"),
                1,
                3,
                new BigDecimal("25000.00"),
                new BigDecimal("62400.00"));
    }
}
