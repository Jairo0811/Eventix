package com.jairomatias.eventix.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jairomatias.eventix.auth.entity.PasswordResetToken;
import com.jairomatias.eventix.auth.event.PasswordResetRequestedEvent;
import com.jairomatias.eventix.auth.repository.PasswordResetTokenRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordRecoveryServiceTest {

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationEventPublisher eventPublisher;
    private PasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new PasswordRecoveryService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                eventPublisher,
                "https://eventix.example",
                Clock.fixed(Instant.parse("2026-08-10T18:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void unknownEmailShouldNotRevealWhetherAccountExists() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        service.requestReset("unknown@example.com");

        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void activeUserShouldReceiveOneTimeResetLinkWithoutPersistingRawToken() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        service.requestReset("USER@EXAMPLE.COM");

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(tokenRepository).deleteAllByUser_Id(10L);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PasswordResetRequestedEvent event =
                (PasswordResetRequestedEvent) eventCaptor.getValue();
        String rawToken = event.resetUrl().substring(event.resetUrl().indexOf("token=") + 6);
        assertTrue(event.resetUrl().startsWith(
                "https://eventix.example/login/reset-password?token="));
        assertNotEquals(rawToken, tokenCaptor.getValue().getTokenHash());
        assertFalse(tokenCaptor.getValue().getTokenHash().isBlank());
    }

    @Test
    void expiredOrUsedTokenShouldBeRejected() {
        when(tokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.empty());

        assertFalse(service.isTokenValid("expired-token"));
    }

    @Test
    void inactiveAccountShouldNotReceiveResetToken() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);
        when(userRepository.findByEmailIgnoreCase("inactive@example.com"))
                .thenReturn(Optional.of(user));

        service.requestReset("inactive@example.com");

        verifyNoInteractions(tokenRepository, eventPublisher);
    }

    @Test
    void validTokenShouldEncodePasswordAndInvalidateAllUserTokens() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPasswordHash()).thenReturn("old-hash");
        PasswordResetToken token = new PasswordResetToken(
                user,
                "stored-hash",
                LocalDateTime.of(2026, 8, 10, 18, 30));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("NewPassword1!", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword1!"))
                .thenReturn("new-hash");

        service.resetPassword(
                "valid-token",
                "NewPassword1!",
                "NewPassword1!");

        verify(user).setPasswordHash("new-hash");
        verify(user).setMustChangePassword(false);
        verify(tokenRepository).deleteAllByUser_Id(10L);
    }

    @Test
    void invalidTokenShouldNeverChangePassword() {
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessRuleException.class,
                () -> service.resetPassword(
                        "invalid-token",
                        "NewPassword1!",
                        "NewPassword1!"));

        verifyNoInteractions(passwordEncoder);
    }
}
