package com.jairomatias.eventix.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoginRedirectsTest {

    @Test
    void acceptsCheckoutAndStorefrontPaths() {
        assertThat(LoginRedirects.safeNext("/my/checkout/events/12")).contains("/my/checkout/events/12");
        assertThat(LoginRedirects.safeNext("/e/7")).contains("/e/7");
        assertThat(LoginRedirects.safeNext("/my")).contains("/my");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://evil.example/phish",
        "//evil.example",
        "/\\evil.example",
        "javascript:alert(1)",
        "/dashboard",
        "/users/1",
        "/my/../users",
        "/e/7?x=<script>",
        "",
        "my/checkout"})
    void rejectsExternalOrUnexpectedTargets(String candidate) {
        assertThat(LoginRedirects.safeNext(candidate)).isEmpty();
    }

    @Test
    void rejectsNullAndOverlongValues() {
        assertThat(LoginRedirects.safeNext(null)).isEmpty();
        assertThat(LoginRedirects.safeNext("/e/" + "a".repeat(300))).isEmpty();
    }
}
