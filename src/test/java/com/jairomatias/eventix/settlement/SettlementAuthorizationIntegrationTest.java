package com.jairomatias.eventix.settlement;

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
class SettlementAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(
            username = "admin@eventix.local",
            roles = "ADMINISTRATOR")
    void administratorCanOpenSettlementList() throws Exception {
        mockMvc.perform(get("/settlements"))
                .andExpect(status().isOk())
                .andExpect(view().name("settlements/list"));
    }

    @Test
    @WithMockUser(
            username = "organizer@eventix.local",
            roles = "ORGANIZER")
    void organizerCannotCreateSettlement() throws Exception {
        mockMvc.perform(post("/settlements").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "operator@eventix.local",
            roles = "OPERATOR")
    void operatorCannotOpenSettlementModule() throws Exception {
        mockMvc.perform(get("/settlements"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserMustAuthenticate() throws Exception {
        mockMvc.perform(get("/settlements"))
                .andExpect(status().is3xxRedirection());
    }
}
