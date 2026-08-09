package com.jairomatias.eventix.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditOutcome;
import com.jairomatias.eventix.audit.service.AuditService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditAuthenticationFailureHandler
        extends SimpleUrlAuthenticationFailureHandler {

    private final AuditService auditService;

    public AuditAuthenticationFailureHandler(AuditService auditService) {
        super("/login?error");
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {
        auditService.recordAuthentication(
                AuditEventType.AUTHENTICATION_FAILURE,
                AuditOutcome.FAILURE,
                request.getParameter("username"),
                request,
                "Credenciales inválidas o cuenta no disponible.");
        super.onAuthenticationFailure(request, response, exception);
    }
}
