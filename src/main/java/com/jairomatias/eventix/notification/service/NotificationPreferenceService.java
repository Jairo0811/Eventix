package com.jairomatias.eventix.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class NotificationPreferenceService {

    private final UserRepository userRepository;

    public NotificationPreferenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean allowsReservationNotifications(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::isReservationNotificationsEnabled)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean allowsEventReminders(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::isEventReminderNotificationsEnabled)
                .orElse(true);
    }
}
