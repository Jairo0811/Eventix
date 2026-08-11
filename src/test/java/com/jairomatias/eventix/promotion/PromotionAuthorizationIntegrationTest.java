package com.jairomatias.eventix.promotion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
class PromotionAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(
            username = "admin@eventix.local",
            roles = "ADMINISTRATOR")
    void administratorCanOpenCouponAdministration() throws Exception {
        mockMvc.perform(get("/promotions/coupons"))
                .andExpect(status().isOk())
                .andExpect(view().name("promotions/coupons/list"));
    }

    @Test
    @WithMockUser(
            username = "organizer@eventix.local",
            roles = "ORGANIZER")
    void organizerCannotOpenCouponAdministration() throws Exception {
        mockMvc.perform(get("/promotions/coupons"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/promotions/coupons"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
