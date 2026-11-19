package com.jairomatias.eventix.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Blocks readiness for a production instance with unsafe or missing settings.
 * Validation deliberately reports field names only, never credentials.
 *
 * This is a startup guard, not proof of bank certification or database recovery.
 */
@Component
@Profile("prod")
public class ProductionReadinessValidator implements ApplicationRunner {

    private final Environment environment;

    public ProductionReadinessValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        require("APP_BASE_URL", validHttpsOrigin(value("APP_BASE_URL")));
        require("DB_URL", validSqlServerTls(value("DB_URL")));
        if (StringUtils.hasText(value("FLYWAY_DB_URL"))) {
            require("FLYWAY_DB_URL", validSqlServerTls(value("FLYWAY_DB_URL")));
        }

        for (String name : Set.of(
                "DB_USERNAME", "DB_PASSWORD",
                "FLYWAY_DB_USERNAME", "FLYWAY_DB_PASSWORD",
                "EVENTIX_ELIGIBILITY_HMAC_SECRET",
                "TICKETING_SIGNING_KEY_ID",
                "TICKETING_SIGNING_PRIVATE_KEY",
                "TICKETING_SIGNING_PUBLIC_KEY")) {
            require(name, StringUtils.hasText(value(name)));
        }
        require("EVENTIX_ELIGIBILITY_HMAC_SECRET",
                value("EVENTIX_ELIGIBILITY_HMAC_SECRET").length() >= 32);
        require("TICKETING_SIGNING_KEY_ID",
                !Objects.equals(value("TICKETING_SIGNING_KEY_ID"), "eventix-dev"));
        require("DB_USERNAME/FLYWAY_DB_USERNAME",
                !value("DB_USERNAME").equals(value("FLYWAY_DB_USERNAME")));
        require("DB_PASSWORD/FLYWAY_DB_PASSWORD",
                !value("DB_PASSWORD").equals(value("FLYWAY_DB_PASSWORD")));
        require("EVENTIX_ALLOW_DEFAULT_BOOTSTRAP_ADMIN", !flag("EVENTIX_ALLOW_DEFAULT_BOOTSTRAP_ADMIN"));
        require("TICKETING_ALLOW_EPHEMERAL_SIGNING_KEY",
                !flag("TICKETING_ALLOW_EPHEMERAL_SIGNING_KEY"));

        if (flag("EVENTIX_CHECKOUT_ENABLED")) {
            require("AZUL_WALLET_ENABLED", flag("AZUL_WALLET_ENABLED"));
            require("AZUL_WALLET_ENVIRONMENT",
                    "PRODUCTION".equalsIgnoreCase(value("AZUL_WALLET_ENVIRONMENT")));
            for (String key : Arrays.asList("AZUL_STORE", "AZUL_AUTH1", "AZUL_AUTH2")) {
                require(key, StringUtils.hasText(value(key)));
            }
        }
    }

    static boolean validSqlServerTls(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String clean = url.toLowerCase(Locale.ROOT);
        if (!clean.startsWith("jdbc:sqlserver://") || clean.contains("trustservercertificate=true")) {
            return false;
        }
        Set<String> segments = Set.copyOf(Arrays.asList(clean.split(";")));
        return segments.contains("encrypt=true")
                && segments.contains("trustservercertificate=false")
                && Arrays.stream(clean.split(";"))
                        .anyMatch(part -> part.startsWith("databasename=")
                                && part.length() > "databasename=".length());
    }

    static boolean validHttpsOrigin(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null && !host.equalsIgnoreCase("localhost")
                    && !host.equals("127.0.0.1")
                    && !host.endsWith(".local")
                    && uri.getUserInfo() == null
                    && (uri.getPath() == null || uri.getPath().isEmpty() || uri.getPath().equals("/"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean flag(String name) {
        return Boolean.parseBoolean(value(name));
    }

    private String value(String key) {
        return environment.getProperty(key, "");
    }

    private static void require(String key, boolean safe) {
        if (!safe) {
            throw new IllegalStateException(
                    "Configuración de producción inválida o incompleta: " + key);
        }
    }
}
