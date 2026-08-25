package com.jairomatias.eventix.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DeploymentConfigurationContractTest {

    @Test
    void eligibilityHmacSecretIsDocumentedBoundAndForwarded()
            throws IOException {
        String environmentExample = read(".env.example");
        String application = read("src/main/resources/application.yml");
        String compose = read("compose.yaml");
        String ci = read(".github/workflows/ci.yml");

        assertThat(environmentExample)
                .contains("EVENTIX_ELIGIBILITY_HMAC_SECRET=")
                .contains("openssl rand -hex 32");
        assertThat(application)
                .contains("hmac-secret: ${EVENTIX_ELIGIBILITY_HMAC_SECRET:}");
        assertThat(compose)
                .contains("EVENTIX_ELIGIBILITY_HMAC_SECRET: "
                        + "${EVENTIX_ELIGIBILITY_HMAC_SECRET:?Define "
                        + "EVENTIX_ELIGIBILITY_HMAC_SECRET}");
        assertThat(ci)
                .contains("EVENTIX_ELIGIBILITY_HMAC_SECRET="
                        + "$(openssl rand -hex 32)");
    }

    @Test
    void actuatorVersionComesFromMavenProjectVersion()
            throws IOException {
        assertThat(read("src/main/resources/application.yml"))
                .contains("version: \"@project.version@\"");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
