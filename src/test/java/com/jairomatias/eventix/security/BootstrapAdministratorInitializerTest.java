package com.jairomatias.eventix.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

class BootstrapAdministratorInitializerTest {

    @Test
    void productionRefusesKnownBootstrapPasswordWithoutReplacement() {
        Fixture fixture = fixture();
        fixture.properties.setAllowDefaultBootstrapAdministrator(false);

        assertThatThrownBy(() -> fixture.initializer.run(
                new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EVENTIX_BOOTSTRAP_ADMIN_PASSWORD");
    }

    @Test
    void rotatesKnownBootstrapPasswordFromEnvironment() throws Exception {
        Fixture fixture = fixture();
        fixture.properties.setAllowDefaultBootstrapAdministrator(false);
        fixture.properties.setBootstrapAdministratorPassword(
                "NuevaClave2026#Segura");
        when(fixture.passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        fixture.initializer.run(new DefaultApplicationArguments());

        verify(fixture.user).setPasswordHash("encoded-password");
        verify(fixture.user).setMustChangePassword(true);
    }

    private Fixture fixture() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        EventixSecurityProperties properties = new EventixSecurityProperties();
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("default-hash");
        when(repository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "admin@eventix.local", "admin"))
                .thenReturn(Optional.of(user));
        when(encoder.matches("Admin123*", "default-hash"))
                .thenReturn(true);
        return new Fixture(
                new BootstrapAdministratorInitializer(
                        repository, encoder, properties),
                encoder,
                properties,
                user);
    }

    private record Fixture(
            BootstrapAdministratorInitializer initializer,
            PasswordEncoder passwordEncoder,
            EventixSecurityProperties properties,
            User user) {
    }
}
