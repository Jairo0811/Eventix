package com.jairomatias.eventix.reservation;

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
class ReservationAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(
            username = "admin@eventix.local",
            roles = "ADMINISTRATOR")
    void administratorCanOpenReservationList() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/list"));
    }

    @Test
    @WithMockUser(
            username = "organizer@eventix.local",
            roles = "ORGANIZER")
    void organizerCannotCreateReservations() throws Exception {
        mockMvc.perform(post("/reservations").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "access@eventix.local",
            roles = "ACCESS_STAFF")
    void accessStaffCannotOpenReservationModule() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().is3xxRedirection());
    }
}
