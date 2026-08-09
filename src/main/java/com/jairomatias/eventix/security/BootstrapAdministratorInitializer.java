package com.jairomatias.eventix.security;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Component
public class BootstrapAdministratorInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            BootstrapAdministratorInitializer.class);
    private static final String DEFAULT_PASSWORD = "Admin123*";
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventixSecurityProperties properties;

    public BootstrapAdministratorInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EventixSecurityProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        User administrator = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        "admin@eventix.local",
                        "admin")
                .orElse(null);
        if (administrator == null
                || !passwordEncoder.matches(
                        DEFAULT_PASSWORD,
                        administrator.getPasswordHash())) {
            return;
        }

        String configuredPassword =
                properties.getBootstrapAdministratorPassword();
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            if (!STRONG_PASSWORD.matcher(configuredPassword).matches()) {
                throw new IllegalStateException(
                        "EVENTIX_BOOTSTRAP_ADMIN_PASSWORD debe tener entre "
                        + "12 y 128 caracteres, mayúscula, minúscula, número "
                        + "y símbolo.");
            }
            administrator.setPasswordHash(
                    passwordEncoder.encode(configuredPassword));
            administrator.setMustChangePassword(true);
            LOGGER.info(
                    "La contraseña del administrador inicial fue rotada desde configuración segura.");
            return;
        }

        if (!properties.isAllowDefaultBootstrapAdministrator()) {
            throw new IllegalStateException(
                    "Define EVENTIX_BOOTSTRAP_ADMIN_PASSWORD antes de iniciar "
                    + "Eventix en producción.");
        }
        LOGGER.warn(
                "El administrador de desarrollo conserva su contraseña temporal. "
                + "Cámbiala antes de publicar el sistema.");
    }
}
