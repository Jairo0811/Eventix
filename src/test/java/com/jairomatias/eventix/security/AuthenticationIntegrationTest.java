package com.jairomatias.eventix.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void administratorCanLoginAndMustChangeTemporaryPassword() throws Exception {
        mockMvc.perform(formLogin()
                        .user("admin@eventix.local")
                        .password("Admin123*"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/change-password?required"))
                .andExpect(authenticated().withUsername("admin@eventix.local"));
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin()
                        .user("admin@eventix.local")
                        .password("incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }
}

