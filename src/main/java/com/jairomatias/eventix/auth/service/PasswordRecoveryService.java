package com.jairomatias.eventix.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.auth.entity.PasswordResetToken;
import com.jairomatias.eventix.auth.event.PasswordResetRequestedEvent;
import com.jairomatias.eventix.auth.repository.PasswordResetTokenRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class PasswordRecoveryService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final String baseUrl;
    private final Clock clock;

    @Autowired
    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this(
                userRepository,
                tokenRepository,
                passwordEncoder,
                eventPublisher,
                baseUrl,
                Clock.systemDefaultZone());
    }

    PasswordRecoveryService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            String baseUrl,
            Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::createTokenAndNotify);
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        return tokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.isUsableAt(now))
                .isPresent();
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword,
            String confirmPassword) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessRuleException("Las contraseñas no coinciden.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new BusinessRuleException(
                        "El enlace de recuperación es inválido o expiró."));

        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "La cuenta no está disponible para recuperación.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessRuleException(
                    "La nueva contraseña debe ser diferente a la actual.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        token.markUsed(now);
        tokenRepository.deleteAllByUser_Id(user.getId());
    }

    private void createTokenAndNotify(User user) {
        tokenRepository.deleteAllByUser_Id(user.getId());

        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken(
                user,
                hash(rawToken),
                LocalDateTime.now(clock).plus(TOKEN_TTL));
        tokenRepository.save(token);

        String resetUrl = baseUrl + "/login/reset-password?token=" + rawToken;
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                user.getEmail(),
                resetUrl));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
