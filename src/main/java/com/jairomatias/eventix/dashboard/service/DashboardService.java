package com.jairomatias.eventix.dashboard.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.dashboard.dto.DashboardSummary;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.service.ReportService;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final SaleRepository saleRepository;
    private final DigitalTicketRepository ticketRepository;
    private final ReportService reportService;

    public DashboardService(
            UserRepository userRepository,
            EventRepository eventRepository,
            ReservationRepository reservationRepository,
            SaleRepository saleRepository,
            DigitalTicketRepository ticketRepository,
            ReportService reportService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.saleRepository = saleRepository;
        this.ticketRepository = ticketRepository;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public DashboardSummary getSummary() {
        LocalDate today = LocalDate.now();
        ReportFilter currentYear = new ReportFilter();
        currentYear.setFrom(today.withDayOfYear(1));
        currentYear.setTo(today);
        ReportDataset report = reportService.generate(currentYear, null);
        long publishedCapacity = eventRepository
                .sumCapacityByStatus(EventStatus.PUBLISHED);
        long allocatedTickets = ticketRepository
                .countByEvent_StatusAndStatusIn(
                        EventStatus.PUBLISHED,
                        List.of(TicketStatus.ACTIVE, TicketStatus.USED));
        return new DashboardSummary(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.INACTIVE),
                userRepository.countByStatus(UserStatus.LOCKED),
                eventRepository.count(),
                eventRepository.countByStatus(EventStatus.DRAFT),
                eventRepository.countByStatus(EventStatus.PUBLISHED),
                eventRepository.countByStatus(EventStatus.CANCELLED),
                eventRepository.countByStatus(EventStatus.FINISHED),
                reservationRepository.count(),
                reservationRepository.countByStatus(
                        ReservationStatus.PENDING),
                reservationRepository.countByStatus(
                        ReservationStatus.CONFIRMED),
                reservationRepository.countByStatus(
                        ReservationStatus.CANCELLED),
                reservationRepository.countByStatus(
                        ReservationStatus.EXPIRED),
                saleRepository.count(),
                saleRepository.countByStatus(SaleStatus.PENDING),
                saleRepository.countByStatus(SaleStatus.PAID),
                saleRepository.countByStatus(SaleStatus.REFUNDED),
                saleRepository.countByStatus(SaleStatus.CANCELLED),
                userRepository.countByRole_NameAndStatus(
                        RoleName.ORGANIZER,
                        UserStatus.ACTIVE),
                Math.max(publishedCapacity - allocatedTickets, 0),
                report.summary(),
                report.byEvent().stream()
                        .filter(row -> row.sales() > 0
                                || row.reservations() > 0)
                        .limit(5)
                        .toList(),
                report.monthlyRevenue());
    }
}
