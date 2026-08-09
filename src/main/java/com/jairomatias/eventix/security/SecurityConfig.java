package com.jairomatias.eventix.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;
    private final AuditAuthenticationFailureHandler authenticationFailureHandler;
    private final AuditLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(
            DatabaseUserDetailsService userDetailsService,
            LoginSuccessHandler loginSuccessHandler,
            ForcePasswordChangeFilter forcePasswordChangeFilter,
            AuditAuthenticationFailureHandler authenticationFailureHandler,
            AuditLogoutSuccessHandler logoutSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.forcePasswordChangeFilter = forcePasswordChangeFilter;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    /**
     * Se declara static para que Spring pueda crear el codificador sin
     * instanciar previamente SecurityConfig, evitando dependencias circulares.
     */
    @Bean
    static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);

        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error/**",
                                "/actuator/health",
                                "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/wallet/apple/**")
                        .permitAll()
                        .requestMatchers("/users/**")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers("/categories/**")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers("/events/*/ticket-types/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "ORGANIZER")
                        .requestMatchers(
                                "/events/new",
                                "/events/*/edit")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "ORGANIZER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/events/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "ORGANIZER")
                        .requestMatchers("/events/**")
                        .authenticated()
                        .requestMatchers(
                                "/reservations/new",
                                "/reservations/*/edit")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/reservations/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR")
                        .requestMatchers("/reservations/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR",
                                "ORGANIZER")
                        .requestMatchers("/sales/new")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/sales/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR")
                        .requestMatchers("/sales/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR",
                                "ORGANIZER")
                        .requestMatchers("/tickets/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR",
                                "ORGANIZER")
                        .requestMatchers("/reports/**")
                        .hasAnyRole("ADMINISTRATOR", "ORGANIZER")
                        .requestMatchers("/audit/**")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/access-control/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR",
                                "ACCESS_STAFF")
                        .requestMatchers("/access-control/**")
                        .hasAnyRole(
                                "ADMINISTRATOR",
                                "OPERATOR",
                                "ORGANIZER",
                                "ACCESS_STAFF")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation(fixation ->
                                fixation.migrateSession())
                        .invalidSessionUrl("/login?expired")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied"))
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/wallet/apple/**"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                + "script-src 'self' https://cdn.jsdelivr.net; "
                                + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                                + "font-src 'self' data: https://cdn.jsdelivr.net; "
                                + "img-src 'self' data: https:; "
                                + "connect-src 'self'; object-src 'none'; "
                                + "base-uri 'self'; frame-ancestors 'none'; "
                                + "form-action 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy",
                                "camera=(self), microphone=(), geolocation=(), payment=()"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)))
                .addFilterAfter(
                        forcePasswordChangeFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
