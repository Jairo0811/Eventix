package com.jairomatias.eventix.ticket.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jairomatias.eventix.audit.service.AuditService;
import com.jairomatias.eventix.security.AuditAuthenticationFailureHandler;
import com.jairomatias.eventix.security.AuditLogoutSuccessHandler;
import com.jairomatias.eventix.security.DatabaseUserDetailsService;
import com.jairomatias.eventix.security.EventixSecurityProperties;
import com.jairomatias.eventix.security.ExternalOidcUserService;
import com.jairomatias.eventix.security.LoginSuccessHandler;
import com.jairomatias.eventix.security.SecurityConfig;
import com.jairomatias.eventix.ticket.dto.TicketDetailsView;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.service.TicketService;

/** Ensures buyer ticket actions never point to administrator-only pages. */
@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, EventixSecurityProperties.class})
class TicketDetailNavigationTest {

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @MockitoBean
    private AuditAuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuditLogoutSuccessHandler logoutSuccessHandler;

    @MockitoBean
    private ExternalOidcUserService externalOidcUserService;

    @Autowired
    private MockMvc mockMvc;

    private static TicketDetailsView ticket() {
        return ticket(TicketStatus.ACTIVE, false, false);
    }

    private static TicketDetailsView ticket(
            TicketStatus status, boolean googleWalletAvailable, boolean appleWalletAvailable) {
        return new TicketDetailsView(
                41L,
                "EVX-41",
                91L,
                "SAL-91",
                7L,
                "Concierto de Año Nuevo",
                LocalDateTime.of(2030, 12, 31, 21, 0),
                LocalDateTime.of(2031, 1, 1, 1, 0),
                "Teatro Nacional",
                "Santo Domingo",
                "Organizador Eventix",
                "Ana Pérez",
                "buyer@example.com",
                "General",
                "Platea",
                "A-1",
                status,
                "ANTI-41",
                "key-1",
                "abcd1234",
                LocalDateTime.of(2030, 11, 4, 12, 0),
                null,
                null,
                null,
                googleWalletAvailable,
                appleWalletAvailable);
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void buyerCanReturnToOwnTicketsWithoutSeeingAdministrativeSalesLinks() throws Exception {
        when(ticketService.findById(41L, "buyer@example.com")).thenReturn(ticket());

        mockMvc.perform(get("/tickets/41"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/detail"))
                .andExpect(content().string(containsString("href=\"/my/tickets\"")))
                .andExpect(content().string(not(containsString("href=\"/tickets\""))))
                .andExpect(content().string(not(containsString("href=\"/sales/91\""))))
                .andExpect(content().string(containsString("SAL-91")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMINISTRATOR")
    void administratorKeepsTicketListingAndAuthorizedSaleLinks() throws Exception {
        when(ticketService.findById(41L, "admin@example.com")).thenReturn(ticket());

        mockMvc.perform(get("/tickets/41"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/tickets\"")))
                .andExpect(content().string(containsString("href=\"/sales/91\"")))
                .andExpect(content().string(not(containsString("href=\"/my/tickets\""))));
    }
    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void activeTicketRendersQrDialogWithLocalScriptAndCollapsibleTechnicalDetails() throws Exception {
        when(ticketService.findById(41L, "buyer@example.com")).thenReturn(ticket());

        mockMvc.perform(get("/tickets/41"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Presenta este QR en el acceso")))
                .andExpect(content().string(containsString("data-ticket-qr-dialog")))
                .andExpect(content().string(containsString("data-ticket-qr-open")))
                .andExpect(content().string(containsString("href=\"/tickets/41/qr\"")))
                .andExpect(content().string(containsString("src=\"/js/ticket-presentation.js\"")))
                .andExpect(content().string(containsString("<details class=\"ticket-pass-technical\"")))
                .andExpect(content().string(containsString("Ver información técnica y de la compra")));
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void inactiveTicketsDoNotOfferQrPresentationOrWalletButtons() throws Exception {
        for (TicketStatus status : new TicketStatus[] {
                TicketStatus.USED, TicketStatus.CANCELLED, TicketStatus.EXPIRED}) {
            when(ticketService.findById(41L, "buyer@example.com"))
                    .thenReturn(ticket(status, true, true));

            mockMvc.perform(get("/tickets/41"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Esta boleta no está activa.")))
                    .andExpect(content().string(not(containsString("data-ticket-qr-dialog"))))
                    .andExpect(content().string(not(containsString("src=\"/tickets/41/qr\""))))
                    .andExpect(content().string(not(containsString("href=\"/tickets/41/wallet/google\""))))
                    .andExpect(content().string(not(containsString("href=\"/tickets/41/wallet/apple\""))));
        }
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void activeTicketKeepsWalletLinksOnlyWhenProvidersAreAvailable() throws Exception {
        when(ticketService.findById(41L, "buyer@example.com"))
                .thenReturn(ticket(TicketStatus.ACTIVE, true, true));

        mockMvc.perform(get("/tickets/41"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/tickets/41/wallet/google\"")))
                .andExpect(content().string(containsString("href=\"/tickets/41/wallet/apple\"")))
                .andExpect(content().string(containsString("href=\"/tickets/41/pdf\"")));
    }

}
