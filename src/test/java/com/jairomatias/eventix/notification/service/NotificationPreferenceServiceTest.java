package com.jairomatias.eventix.notification.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationPreferenceServiceTest {

    @Test
    void respectsRegisteredUserPreference() {
        UserRepository repository = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.isReservationNotificationsEnabled()).thenReturn(false);
        when(repository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));
        NotificationPreferenceService service =
                new NotificationPreferenceService(repository);

        assertThat(service.allowsReservationNotifications("user@example.com"))
                .isFalse();
    }

    @Test
    void keepsEssentialGuestFlowEnabledWhenNoAccountMatches() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("guest@example.com"))
                .thenReturn(Optional.empty());
        NotificationPreferenceService service =
                new NotificationPreferenceService(repository);

        assertThat(service.allowsReservationNotifications("guest@example.com"))
                .isTrue();
        assertThat(service.allowsEventReminders("guest@example.com"))
                .isTrue();
    }
}
