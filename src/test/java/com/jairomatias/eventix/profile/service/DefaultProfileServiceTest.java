package com.jairomatias.eventix.profile.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.profile.dto.ProfileUpdateForm;
import com.jairomatias.eventix.profile.dto.ProfileUpdateResult;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProfileServiceTest {

    private UserRepository userRepository;
    private DefaultProfileService profileService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileService = new DefaultProfileService(userRepository);
        user = mock(User.class);
        when(user.getId()).thenReturn(8L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "user@example.com",
                "user@example.com"))
                .thenReturn(Optional.of(user));
    }

    @Test
    void updatesOnlyAllowedProfileFields() {
        ProfileUpdateForm form = validForm("USER@example.com");

        ProfileUpdateResult result = profileService.updateOwnProfile(
                "user@example.com",
                form);

        assertThat(result.requiresReauthentication()).isFalse();
        verify(user).setFirstName("Ana");
        verify(user).setLastName("Pérez");
        verify(user).setEmail("user@example.com");
        verify(user).setPhone("809-555-0100");
        verify(user).setReservationNotificationsEnabled(false);
        verify(user).setEventReminderNotificationsEnabled(true);
        verify(user, never()).setRole(any());
    }

    @Test
    void changingEmailRequiresReauthentication() {
        ProfileUpdateForm form = validForm("new@example.com");

        ProfileUpdateResult result = profileService.updateOwnProfile(
                "user@example.com",
                form);

        assertThat(result.requiresReauthentication()).isTrue();
        verify(user).setEmail("new@example.com");
    }

    @Test
    void rejectsEmailAlreadyOwnedByAnotherAccount() {
        ProfileUpdateForm form = validForm("existing@example.com");
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(
                "existing@example.com",
                8L))
                .thenReturn(true);

        assertThatThrownBy(() -> profileService.updateOwnProfile(
                "user@example.com",
                form))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("correo");

        verify(user, never()).setEmail(any());
    }

    private ProfileUpdateForm validForm(String email) {
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFirstName(" Ana ");
        form.setLastName(" Pérez ");
        form.setEmail(email);
        form.setPhone(" 809-555-0100 ");
        form.setReservationNotificationsEnabled(false);
        form.setEventReminderNotificationsEnabled(true);
        return form;
    }
}
