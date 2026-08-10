package com.jairomatias.eventix.commerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.commerce.dto.RevenueBreakdown;
import com.jairomatias.eventix.commerce.repository.PlatformRevenueRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class PlatformRevenueService {

    private final PlatformRevenueRepository revenueRepository;
    private final UserRepository userRepository;

    public PlatformRevenueService(
            PlatformRevenueRepository revenueRepository,
            UserRepository userRepository) {
        this.revenueRepository = revenueRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public RevenueBreakdown getRevenue(String authenticatedLogin) {
        User actor = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        authenticatedLogin,
                        authenticatedLogin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));

        Long organizerId = actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;

        BigDecimal gross = amount(revenueRepository.sumGrossPaid(organizerId));
        BigDecimal platform = amount(revenueRepository.sumPlatformRevenue(organizerId));
        BigDecimal organizerNet = amount(revenueRepository.sumOrganizerNet(organizerId));
        BigDecimal refunded = amount(revenueRepository.sumRefunded(organizerId));
        BigDecimal effectiveRate = gross.signum() == 0
                ? BigDecimal.ZERO
                : platform.divide(gross, 4, RoundingMode.HALF_UP);

        return new RevenueBreakdown(
                gross,
                platform,
                organizerNet,
                refunded,
                effectiveRate);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
