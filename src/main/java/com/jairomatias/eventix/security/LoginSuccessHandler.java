package com.jairomatias.eventix.security;

import java.io.IOException;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.user.service.UserService;
import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditOutcome;
import com.jairomatias.eventix.audit.service.AuditService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final AuditService auditService;

    public LoginSuccessHandler(
            @Lazy UserService userService,
            AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        userService.recordSuccessfulLogin(authentication.getName());
        auditService.recordAuthentication(
                AuditEventType.LOGIN,
                AuditOutcome.SUCCESS,
                authentication.getName(),
                request,
                "Inicio de sesión correcto.");

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        String destination;
        if (principal.isMustChangePassword()) {
            destination = "/auth/change-password?required";
        } else if ("USER".equals(principal.getRoleName())) {
            destination = "/my";
        } else {
            destination = "/dashboard";
        }

        response.sendRedirect(
                request.getContextPath() + destination);
    }
}
