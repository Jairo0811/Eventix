package com.jairomatias.eventix.dashboard.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.dashboard.dto.DashboardSummary;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DashboardService {

    private final UserRepository userRepository;

    public DashboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public DashboardSummary getSummary() {
        return new DashboardSummary(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.INACTIVE),
                userRepository.countByStatus(UserStatus.LOCKED));
    }
}
