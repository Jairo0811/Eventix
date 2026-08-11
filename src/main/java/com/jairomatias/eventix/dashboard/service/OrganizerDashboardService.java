package com.jairomatias.eventix.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.dashboard.dto.OrganizerDashboardMetrics;
import com.jairomatias.eventix.dashboard.dto.OrganizerDashboardSummary;
import com.jairomatias.eventix.dashboard.repository.OrganizerDashboardRepository;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.service.ReportService;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class OrganizerDashboardService {

    private static final int RATE_SCALE = 2;

    private final OrganizerDashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final Clock clock;

    @Autowired
    public OrganizerDashboardService(
            OrganizerDashboardRepository dashboardRepository,
            UserRepository userRepository,
            ReportService reportService) {
        this(
                dashboardRepository,
                userRepository,
                reportService,
                Clock.systemDefaultZone());
    }

    OrganizerDashboardService(
            OrganizerDashboardRepository dashboardRepository,
            UserRepository userRepository,
            ReportService reportService,
            Clock clock) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ORGANIZER')")
    public OrganizerDashboardSummary getSummary(String authenticatedLogin) {
        User organizer = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        authenticatedLogin,
                        authenticatedLogin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el organizador autenticado."));
        if (organizer.getRole().getName() != RoleName.ORGANIZER) {
            throw new BusinessRuleException(
                    "El centro comercial requiere un organizador.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        OrganizerDashboardMetrics metrics = dashboardRepository.loadMetrics(
                organizer.getId(),
                now);
        ReportDataset report = currentYearReport(organizer.getId());
        return new OrganizerDashboardSummary(
                metrics.upcomingEvents(),
                metrics.publishedEvents(),
                metrics.ticketsSold(),
                metrics.activeReservations(),
                percentage(metrics.ticketsSold(), metrics.publishedCapacity()),
                metrics.grossSales(),
                metrics.discounts(),
                metrics.refunds(),
                metrics.platformCommission(),
                metrics.estimatedNet(),
                metrics.pendingSettlements(),
                metrics.paidSettlements(),
                metrics.pendingSettlementNet(),
                metrics.paidSettlementNet(),
                dashboardRepository.loadUpcomingEvents(
                        organizer.getId(),
                        now),
                report.monthlyRevenue());
    }

    private ReportDataset currentYearReport(Long organizerId) {
        LocalDate today = LocalDate.now(clock);
        ReportFilter filter = new ReportFilter();
        filter.setFrom(today.withDayOfYear(1));
        filter.setTo(today);
        return reportService.generate(filter, organizerId);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(new BigDecimal("100"))
                .divide(
                        BigDecimal.valueOf(denominator),
                        RATE_SCALE,
                        RoundingMode.HALF_UP);
    }
}
