package com.jairomatias.eventix.ticket;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(
            username = "admin@eventix.local",
            roles = "ADMINISTRATOR")
    void administratorCanOpenTicketModule() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"));
    }

    @Test
    @WithMockUser(
            username = "admin@eventix.local",
            roles = "ACCESS_STAFF")
    void accessStaffCanOpenAccessControl() throws Exception {
        mockMvc.perform(get("/access-control"))
                .andExpect(status().isOk())
                .andExpect(view().name("access/index"));
    }

    @Test
    @WithMockUser(
            username = "organizer@eventix.local",
            roles = "ORGANIZER")
    void organizerCannotPostScans() throws Exception {
        mockMvc.perform(post("/access-control/scan").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void appleWalletWebServiceDoesNotRedirectToLogin() throws Exception {
        mockMvc.perform(get(
                        "/api/wallet/apple/v1/devices/device/"
                        + "registrations/pass.eventix"))
                .andExpect(status().isNotFound());
    }
}
