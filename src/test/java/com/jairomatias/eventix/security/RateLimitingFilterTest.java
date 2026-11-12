package com.jairomatias.eventix.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterTest {

    @Test
    void rejectsLoginAttemptsAboveConfiguredLimit() throws Exception {
        EventixSecurityProperties properties = new EventixSecurityProperties();
        properties.getRateLimit().setLoginRequestsPerMinute(2);
        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                Clock.fixed(Instant.parse("2026-08-08T12:00:10Z"), ZoneOffset.UTC));

        assertThat(execute(filter, "/login").getStatus()).isEqualTo(200);
        assertThat(execute(filter, "/login").getStatus()).isEqualTo(200);
        MockHttpServletResponse rejected = execute(filter, "/login");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("50");
        assertThat(rejected.getContentAsString()).contains("Demasiadas solicitudes");
    }

    @Test
    void rejectsPasswordRecoveryRequestsAboveLoginLimit() throws Exception {
        EventixSecurityProperties properties = new EventixSecurityProperties();
        properties.getRateLimit().setLoginRequestsPerMinute(1);
        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                Clock.fixed(Instant.parse("2026-08-08T12:00:10Z"), ZoneOffset.UTC));

        assertThat(execute(filter, "/login/forgot-password").getStatus())
                .isEqualTo(200);

        assertThat(execute(filter, "/login/forgot-password").getStatus())
                .isEqualTo(429);
    }

    @Test
    void countsEachClientAddressSeparately() throws Exception {
        EventixSecurityProperties properties = new EventixSecurityProperties();
        properties.getRateLimit().setLoginRequestsPerMinute(1);
        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                Clock.fixed(Instant.parse("2026-08-08T12:00:10Z"), ZoneOffset.UTC));

        assertThat(execute(filter, "/login", "192.0.2.10").getStatus()).isEqualTo(200);
        assertThat(execute(filter, "/login", "192.0.2.10").getStatus()).isEqualTo(429);
        // Otro cliente no comparte el cupo. En produccion esto exige que
        // request.getRemoteAddr() sea la IP real (forward-headers-strategy: native).
        assertThat(execute(filter, "/login", "192.0.2.11").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse execute(
            RateLimitingFilter filter,
            String path)
            throws Exception {
        return execute(filter, path, "192.0.2.10");
    }

    private MockHttpServletResponse execute(
            RateLimitingFilter filter,
            String path,
            String remoteAddress)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
