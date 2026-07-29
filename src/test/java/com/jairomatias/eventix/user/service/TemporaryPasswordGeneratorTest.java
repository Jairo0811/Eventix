package com.jairomatias.eventix.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @Test
    void generatedPasswordMeetsSecurityPolicy() {
        String password = generator.generate();

        assertThat(password)
                .hasSize(14)
                .matches(".*[A-Z].*")
                .matches(".*[a-z].*")
                .matches(".*\\d.*")
                .matches(".*[^A-Za-z0-9].*");
    }

    @Test
    void generatedPasswordsAreNotRepeated() {
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }
}
