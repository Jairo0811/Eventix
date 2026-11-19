package com.jairomatias.eventix.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;

import com.jairomatias.eventix.payment.gateway.SimulatedPaymentGateway;

class ProductionReadinessValidatorTest {

    private MockEnvironment safeEnvironment() {
        return new MockEnvironment()
                .withProperty("APP_BASE_URL", "https://eventix.example.com")
                .withProperty("DB_URL", "jdbc:sqlserver://db.example.com:1433;"
                        + "databaseName=EventixDb;encrypt=true;trustServerCertificate=false")
                .withProperty("DB_USERNAME", "eventix_app")
                .withProperty("DB_PASSWORD", "APP-sample-not-real")
                .withProperty("FLYWAY_DB_USERNAME", "eventix_migrator")
                .withProperty("FLYWAY_DB_PASSWORD", "MIGRATOR-sample-not-real")
                .withProperty("EVENTIX_ELIGIBILITY_HMAC_SECRET",
                        "test-only-0123456789-abcdefghijklmnopqrstuvwxyz")
                .withProperty("TICKETING_SIGNING_KEY_ID", "eventix-signing-2026")
                .withProperty("TICKETING_SIGNING_PRIVATE_KEY", "fixture-private-key")
                .withProperty("TICKETING_SIGNING_PUBLIC_KEY", "fixture-public-key")
                .withProperty("TICKETING_ALLOW_EPHEMERAL_SIGNING_KEY", "false")
                .withProperty("EVENTIX_ALLOW_DEFAULT_BOOTSTRAP_ADMIN", "false")
                .withProperty("EVENTIX_CHECKOUT_ENABLED", "false")
                .withProperty("AZUL_WALLET_ENABLED", "false");
    }

    @Test
    void acceptsSafeProductionConfigurationWithCheckoutDisabled() {
        new ProductionReadinessValidator(safeEnvironment()).run(null);
        assertThat(ProductionReadinessValidator.class.getAnnotation(Profile.class).value())
                .containsExactly("prod");
        assertThat(SimulatedPaymentGateway.class.getAnnotation(Profile.class).value())
                .containsExactly("!prod");
    }

    @Test
    void rejectsUnencryptedOrUntrustedSqlServer() {
        MockEnvironment environment = safeEnvironment().withProperty(
                "DB_URL", "jdbc:sqlserver://db.example.com;"
                        + "databaseName=EventixDb;encrypt=true;trustServerCertificate=true");
        assertThatThrownBy(() -> new ProductionReadinessValidator(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL")
                .hasMessageNotContaining("APP-sample-not-real");
    }

    @Test
    void rejectsHttpPublicAddressAndLocalhost() {
        assertThat(ProductionReadinessValidator.validHttpsOrigin("http://eventix.example.com")).isFalse();
        assertThat(ProductionReadinessValidator.validHttpsOrigin("https://localhost:8080")).isFalse();
        assertThat(ProductionReadinessValidator.validHttpsOrigin("https://eventix.example.com")).isTrue();
    }

    @Test
    void requiresDistinctDbAccountsAndStableSigningKeys() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("FLYWAY_DB_USERNAME", "eventix_app");
        assertThatThrownBy(() -> new ProductionReadinessValidator(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_USERNAME/FLYWAY_DB_USERNAME");

        assertThatThrownBy(() -> new ProductionReadinessValidator(safeEnvironment()
                        .withProperty("TICKETING_SIGNING_PRIVATE_KEY", "")).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TICKETING_SIGNING_PRIVATE_KEY");
    }

    @Test
    void checkoutCannotOpenWithoutAConfiguredProductionWalletProvider() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("EVENTIX_CHECKOUT_ENABLED", "true");
        assertThatThrownBy(() -> new ProductionReadinessValidator(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZUL_WALLET_ENABLED");

        environment.withProperty("AZUL_WALLET_ENABLED", "true")
                .withProperty("AZUL_WALLET_ENVIRONMENT", "PRODUCTION")
                .withProperty("AZUL_STORE", "test-only-merchant")
                .withProperty("AZUL_AUTH1", "test-auth-one")
                .withProperty("AZUL_AUTH2", "test-auth-two");
        new ProductionReadinessValidator(environment).run(null);
    }
}
