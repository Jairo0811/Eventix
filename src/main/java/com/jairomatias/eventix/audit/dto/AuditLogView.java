package com.jairomatias.eventix.audit.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditOutcome;

public record AuditLogView(
        Long id,
        LocalDateTime occurredAt,
        String actorUsername,
        AuditEventType eventType,
        String action,
        String entityType,
        String entityId,
        AuditOutcome outcome,
        String httpMethod,
        String requestPath,
        String ipAddress,
        String userAgent,
        String correlationId,
        String details) {
}
