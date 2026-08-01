package com.jairomatias.eventix.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;

    public SecurityConfig(
            DatabaseUserDetailsService userDetailsService,
            LoginSuccessHandler loginSuccessHandler,
            ForcePasswordChangeFilter forcePasswordChangeFilter) {
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.forcePasswordChangeFilter = forcePasswordChangeFilter;
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
                                "/error/**")
                        .permitAll()
                        .requestMatchers("/users/**")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers("/categories/**")
                        .hasRole("ADMINISTRATOR")
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
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
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
                .csrf(withDefaults())
                .addFilterAfter(
                        forcePasswordChangeFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
