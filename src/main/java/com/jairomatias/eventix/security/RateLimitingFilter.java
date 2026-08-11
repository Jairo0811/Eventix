package com.jairomatias.eventix.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of(
            "POST", "PUT", "PATCH", "DELETE");
    private static final long WINDOW_SECONDS = 60;

    private final EventixSecurityProperties properties;
    private final Clock clock;
    private final Map<String, WindowCounter> counters =
            new ConcurrentHashMap<>();
    private final AtomicInteger cleanupTicker = new AtomicInteger();

    @Autowired
    public RateLimitingFilter(EventixSecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimitingFilter(
            EventixSecurityProperties properties,
            Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        LimitRule rule = rule(request);
        if (rule == null || !properties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = Instant.now(clock).getEpochSecond();
        long window = now / WINDOW_SECONDS;
        String client = request.getRemoteAddr() == null
                ? "unknown"
                : request.getRemoteAddr();
        String key = rule.scope + ':' + client;
        if (counters.size()
                >= properties.getRateLimit().getMaximumTrackedClients()
                && !counters.containsKey(key)) {
            key = rule.scope + ":overflow";
        }
        final String counterKey = key;
        WindowCounter counter = counters.compute(counterKey, (ignored, current) -> {
            if (current == null || current.window != window) {
                return new WindowCounter(window);
            }
            current.count.incrementAndGet();
            return current;
        });

        if ((cleanupTicker.incrementAndGet() & 255) == 0) {
            counters.entrySet().removeIf(entry -> entry.getValue().window < window);
        }
        if (counter.count.get() > rule.maximum) {
            long retryAfter = WINDOW_SECONDS - (now % WINDOW_SECONDS);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Demasiadas solicitudes. Intenta nuevamente más tarde.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private LimitRule rule(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod()) && "/login".equals(path)) {
            return new LimitRule(
                    "login",
                    properties.getRateLimit().getLoginRequestsPerMinute());
        }
        if ("POST".equals(request.getMethod())
                && "/login/forgot-password".equals(path)) {
            return new LimitRule(
                    "password-recovery",
                    properties.getRateLimit().getLoginRequestsPerMinute());
        }
        if (path.startsWith("/api/wallet/apple/")) {
            return new LimitRule(
                    "wallet",
                    properties.getRateLimit().getWalletRequestsPerMinute());
        }
        if (MUTATING_METHODS.contains(request.getMethod())) {
            return new LimitRule(
                    "mutation",
                    properties.getRateLimit().getMutationRequestsPerMinute());
        }
        return null;
    }

    private record LimitRule(String scope, int maximum) {
    }

    private static final class WindowCounter {
        private final long window;
        private final AtomicInteger count = new AtomicInteger(1);

        private WindowCounter(long window) {
            this.window = window;
        }
    }
}
