package com.jairomatias.eventix.audit.web;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.jairomatias.eventix.audit.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditTrailInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING_METHODS = Set.of(
            "POST", "PUT", "PATCH", "DELETE");

    private final AuditService auditService;

    public AuditTrailInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        if (MUTATING_METHODS.contains(request.getMethod())
                && !isSpecializedAuditPath(request.getRequestURI())) {
            auditService.recordHttpMutation(
                    request,
                    response.getStatus(),
                    exception);
        }
    }

    private boolean isSpecializedAuditPath(String path) {
        return path.startsWith("/api/wallet/apple")
                || "/login".equals(path)
                || "/logout".equals(path);
    }
}
