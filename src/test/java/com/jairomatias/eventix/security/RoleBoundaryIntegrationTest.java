package com.jairomatias.eventix.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class RoleBoundaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user@eventix.local", roles = "USER")
    void userCannotOpenBackofficeDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@eventix.local", roles = "USER")
    void userCannotOpenOperationalTicketList() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@eventix.local", roles = "USER")
    void userCannotOpenOperationalModules() throws Exception {
        mockMvc.perform(get("/reservations")).andExpect(status().isForbidden());
        mockMvc.perform(get("/sales")).andExpect(status().isForbidden());
        mockMvc.perform(get("/reports")).andExpect(status().isForbidden());
        mockMvc.perform(get("/audit")).andExpect(status().isForbidden());
        mockMvc.perform(get("/access-control")).andExpect(status().isForbidden());
        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/categories")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "organizer@eventix.local", roles = "ORGANIZER")
    void organizerCannotOpenAdministratorModules() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/categories")).andExpect(status().isForbidden());
        mockMvc.perform(get("/audit")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isForbidden());
    }
}
