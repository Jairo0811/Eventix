package com.jairomatias.eventix.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase6AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void administratorCanOpenReportsAndAudit() throws Exception {
        mockMvc.perform(get("/reports").with(user(
                        principal(1L, RoleName.ADMINISTRATOR))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/audit").with(user(
                        principal(1L, RoleName.ADMINISTRATOR))))
                .andExpect(status().isOk());
    }

    @Test
    void organizerCanOpenReportsButNotCentralAudit() throws Exception {
        mockMvc.perform(get("/reports").with(user(
                        principal(7L, RoleName.ORGANIZER))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/audit").with(user(
                        principal(7L, RoleName.ORGANIZER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotOpenExecutiveReports() throws Exception {
        mockMvc.perform(get("/reports").with(user(
                        principal(8L, RoleName.OPERATOR))))
                .andExpect(status().isForbidden());
    }

    private UserPrincipal principal(Long id, RoleName roleName) {
        User account = mock(User.class);
        Role role = mock(Role.class);
        when(account.getId()).thenReturn(id);
        when(account.getFullName()).thenReturn("Usuario de prueba");
        when(account.getEmail()).thenReturn(
                roleName.name().toLowerCase() + "@eventix.test");
        when(account.getPasswordHash()).thenReturn("ignored");
        when(account.getRole()).thenReturn(role);
        when(account.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(role.getName()).thenReturn(roleName);
        return UserPrincipal.from(account);
    }
}
