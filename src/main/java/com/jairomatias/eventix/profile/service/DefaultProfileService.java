package com.jairomatias.eventix.profile.service;

import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.profile.dto.ProfileAccountView;
import com.jairomatias.eventix.profile.dto.ProfileUpdateForm;
import com.jairomatias.eventix.profile.dto.ProfileUpdateResult;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultProfileService implements ProfileService {

    private final UserRepository userRepository;

    public DefaultProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProfileAccountView findOwnProfile(String authenticatedLogin) {
        User user = findAuthenticatedUser(authenticatedLogin);
        return new ProfileAccountView(
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProfileUpdateForm getOwnUpdateForm(String authenticatedLogin) {
        User user = findAuthenticatedUser(authenticatedLogin);
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setReservationNotificationsEnabled(
                user.isReservationNotificationsEnabled());
        form.setEventReminderNotificationsEnabled(
                user.isEventReminderNotificationsEnabled());
        return form;
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ProfileUpdateResult updateOwnProfile(
            String authenticatedLogin,
            ProfileUpdateForm form) {
        User user = findAuthenticatedUser(authenticatedLogin);
        String normalizedEmail = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(
                normalizedEmail,
                user.getId())) {
            throw new DuplicateResourceException(
                    "email",
                    "Ya existe una cuenta con ese correo.");
        }

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(normalizedEmail);
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(normalizeNullable(form.getPhone()));
        user.setReservationNotificationsEnabled(
                form.isReservationNotificationsEnabled());
        user.setEventReminderNotificationsEnabled(
                form.isEventReminderNotificationsEnabled());
        return new ProfileUpdateResult(emailChanged);
    }

    private User findAuthenticatedUser(String authenticatedLogin) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        authenticatedLogin,
                        authenticatedLogin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La cuenta autenticada ya no está disponible."));
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
