package com.jairomatias.eventix.dashboard.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.dashboard.dto.DashboardSummary;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public DashboardService(
            UserRepository userRepository,
            EventRepository eventRepository,
            ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public DashboardSummary getSummary() {
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
                        ReservationStatus.EXPIRED));
    }
}
