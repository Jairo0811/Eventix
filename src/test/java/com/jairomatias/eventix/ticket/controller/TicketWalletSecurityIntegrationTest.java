package com.jairomatias.eventix.ticket.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jairomatias.eventix.security.AuditAuthenticationFailureHandler;
import com.jairomatias.eventix.security.AuditLogoutSuccessHandler;
import com.jairomatias.eventix.security.DatabaseUserDetailsService;
import com.jairomatias.eventix.security.ExternalOidcUserService;
import com.jairomatias.eventix.security.ForcePasswordChangeFilter;
import com.jairomatias.eventix.security.LoginSuccessHandler;
import com.jairomatias.eventix.security.SecurityConfig;
import com.jairomatias.eventix.ticket.dto.TicketDetailsView;
import com.jairomatias.eventix.ticket.service.TicketService;

@WebMvcTest(TicketController.class)
@Import(SecurityConfig.class)
class TicketWalletSecurityIntegrationTest {

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @MockitoBean
    private ForcePasswordChangeFilter forcePasswordChangeFilter;

    @MockitoBean
    private AuditAuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuditLogoutSuccessHandler logoutSuccessHandler;

    @MockitoBean
    private ExternalOidcUserService externalOidcUserService;

    private final MockMvc mockMvc;

    TicketWalletSecurityIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void googleWalletRedirectKeepsPrivateSecurityHeaders() throws Exception {
        when(ticketService.createGoogleWalletUrl(41L, "buyer@example.com"))
                .thenReturn("https://pay.google.com/gp/v/save/test-token");

        mockMvc.perform(get("/tickets/41/wallet/google"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        containsString("no-store")))
                .andExpect(header().string(
                        "Referrer-Policy",
                        containsString("no-referrer")));
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void appleWalletDownloadKeepsPrivateSecurityHeaders() throws Exception {
        TicketDetailsView details = mock(TicketDetailsView.class);
        when(details.uniqueCode()).thenReturn("TKT-ABC");
        when(ticketService.findById(41L, "buyer@example.com"))
                .thenReturn(details);
        when(ticketService.createAppleWalletPass(41L, "buyer@example.com"))
                .thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/tickets/41/wallet/apple"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        containsString("no-store")))
                .andExpect(header().string(
                        "Referrer-Policy",
                        containsString("no-referrer")));
    }

    @Test
    void walletRoutesRequireAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/tickets/41/wallet/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        containsString("/login")));
    }
}
