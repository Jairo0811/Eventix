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

        assertThat(execute(filter).getStatus()).isEqualTo(200);
        assertThat(execute(filter).getStatus()).isEqualTo(200);
        MockHttpServletResponse rejected = execute(filter);

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("50");
        assertThat(rejected.getContentAsString()).contains("Demasiadas solicitudes");
    }

    private MockHttpServletResponse execute(RateLimitingFilter filter)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/login");
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
