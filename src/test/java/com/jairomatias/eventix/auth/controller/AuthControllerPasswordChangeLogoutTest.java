package com.jairomatias.eventix.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.BeanPropertyBindingResult;

import com.jairomatias.eventix.auth.service.PasswordRecoveryService;
import com.jairomatias.eventix.security.EventixSecurityProperties;
import com.jairomatias.eventix.user.dto.ChangePasswordForm;
import com.jairomatias.eventix.user.service.UserService;

import jakarta.servlet.http.Cookie;

class AuthControllerPasswordChangeLogoutTest {

    @Test
    void successfulPasswordChangeDeletesStaleSessionCookieBeforeLoginRedirect() {
        UserService users = mock(UserService.class);
        AuthController controller = new AuthController(
                users,
                mock(PasswordRecoveryService.class),
                mock(EventixSecurityProperties.class));

        ChangePasswordForm form = new ChangePasswordForm();
        form.setCurrentPassword("OldTestPassword1!");
        form.setNewPassword("NewTestPassword2!");
        form.setConfirmPassword("NewTestPassword2!");

        var binding = new BeanPropertyBindingResult(form, "changePasswordForm");
        var request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setCookies(new Cookie("JSESSIONID", "expired-session-value"));
        var response = new MockHttpServletResponse();
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@eventix.local",
                "not-used",
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));

        String view = controller.changePassword(
                form, binding, authentication, request, response);

        verify(users).changeOwnPassword("admin@eventix.local", form);
        assertThat(view).isEqualTo("redirect:/login?passwordChanged");
        assertThat(session.isInvalid()).isTrue();
        Cookie cleared = response.getCookie("JSESSIONID");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(cleared.getPath()).isEqualTo("/");
    }
}
