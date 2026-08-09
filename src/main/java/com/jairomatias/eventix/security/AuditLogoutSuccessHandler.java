package com.jairomatias.eventix.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditOutcome;
import com.jairomatias.eventix.audit.service.AuditService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final AuditService auditService;

    public AuditLogoutSuccessHandler(AuditService auditService) {
        this.auditService = auditService;
        setDefaultTargetUrl("/login?logout");
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {
        auditService.recordAuthentication(
                AuditEventType.LOGOUT,
                AuditOutcome.SUCCESS,
                authentication == null ? null : authentication.getName(),
                request,
                "Sesión cerrada correctamente.");
        super.onLogoutSuccess(request, response, authentication);
    }
}
