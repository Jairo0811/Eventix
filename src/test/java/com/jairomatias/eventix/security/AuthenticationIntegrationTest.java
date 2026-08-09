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

import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.repository.AuditLogRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void administratorCanLoginAndMustChangeTemporaryPassword() throws Exception {
        mockMvc.perform(formLogin()
                        .user("admin@eventix.local")
                        .password("Admin123*"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/change-password?required"))
                .andExpect(authenticated().withUsername("admin@eventix.local"));

        org.assertj.core.api.Assertions.assertThat(
                auditLogRepository.countByEventType(AuditEventType.LOGIN))
                .isPositive();
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin()
                        .user("admin@eventix.local")
                        .password("incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        org.assertj.core.api.Assertions.assertThat(
                auditLogRepository.countByEventType(
                        AuditEventType.AUTHENTICATION_FAILURE))
                .isPositive();
    }

    @Test
    void loginPublishesSecurityAndCorrelationHeaders() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders.get("/login"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result
                        .MockMvcResultMatchers.header()
                        .string("Content-Security-Policy",
                                org.hamcrest.Matchers.containsString(
                                        "default-src 'self'")))
                .andExpect(org.springframework.test.web.servlet.result
                        .MockMvcResultMatchers.header()
                        .exists("X-Correlation-ID"));
    }
}
