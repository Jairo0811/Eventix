package com.jairomatias.eventix.audit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jairomatias.eventix.audit.dto.AuditLogView;
import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditLog;
import com.jairomatias.eventix.audit.entity.AuditOutcome;
import com.jairomatias.eventix.audit.repository.AuditLogRepository;
import com.jairomatias.eventix.observability.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class DefaultAuditService implements AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DefaultAuditService.class);
    private static final Pattern ENTITY_ID = Pattern.compile("/(\\d+)(?:/|$)");

    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate transactionTemplate;

    public DefaultAuditService(
            AuditLogRepository auditLogRepository,
            PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition
                        .PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void recordHttpMutation(
            HttpServletRequest request,
            int status,
            Exception exception) {
        String path = request.getRequestURI();
        AuditEventType eventType = classify(path);
        AuditOutcome outcome = status == 403
                ? AuditOutcome.DENIED
                : status >= 400 || exception != null
                        ? AuditOutcome.FAILURE
                        : AuditOutcome.SUCCESS;
        persist(new AuditLog(
                LocalDateTime.now(),
                currentUsername(),
                eventType,
                request.getMethod() + " " + path,
                entityType(path),
                entityId(path),
                outcome,
                request.getMethod(),
                path,
                request.getRemoteAddr(),
                header(request, "User-Agent", 300),
                correlationId(request),
                exception == null
                        ? "HTTP " + status
                        : truncate(exception.getClass().getSimpleName(), 1000)));
    }

    @Override
    public void recordAuthentication(
            AuditEventType eventType,
            AuditOutcome outcome,
            String username,
            HttpServletRequest request,
            String details) {
        persist(new AuditLog(
                LocalDateTime.now(),
                normalize(username),
                eventType,
                eventType.name(),
                "AUTHENTICATION",
                null,
                outcome,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                header(request, "User-Agent", 300),
                correlationId(request),
                truncate(details, 1000)));
    }

    @Override
    public void recordExport(
            String format,
            String username,
            HttpServletRequest request) {
        persist(new AuditLog(
                LocalDateTime.now(),
                normalize(username),
                AuditEventType.EXPORT,
                "EXPORT_REPORT_" + format.toUpperCase(Locale.ROOT),
                "REPORT",
                null,
                AuditOutcome.SUCCESS,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                header(request, "User-Agent", 300),
                correlationId(request),
                "Reporte exportado en formato " + format.toUpperCase(Locale.ROOT)));
    }

    @Override
    public void recordError(
            Throwable exception,
            HttpServletRequest request) {
        if (request == null) {
            return;
        }
        persist(new AuditLog(
                LocalDateTime.now(),
                currentUsername(),
                AuditEventType.ERROR,
                exception.getClass().getSimpleName(),
                entityType(request.getRequestURI()),
                entityId(request.getRequestURI()),
                AuditOutcome.FAILURE,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                header(request, "User-Agent", 300),
                correlationId(request),
                truncate(exception.getMessage(), 1000)));
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<AuditLogView> findAll(
            String term,
            AuditEventType eventType,
            AuditOutcome outcome,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay();
        return auditLogRepository.search(
                        term == null ? "" : term.trim(),
                        eventType,
                        outcome,
                        fromDate,
                        toDate,
                        pageable)
                .map(this::toView);
    }

    private void persist(AuditLog auditLog) {
        try {
            transactionTemplate.executeWithoutResult(
                    status -> auditLogRepository.saveAndFlush(auditLog));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "No se pudo persistir el evento de auditoría {}.",
                    auditLog.getAction(),
                    exception);
        }
    }

    private AuditLogView toView(AuditLog auditLog) {
        return new AuditLogView(
                auditLog.getId(),
                auditLog.getOccurredAt(),
                auditLog.getActorUsername(),
                auditLog.getEventType(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getOutcome(),
                auditLog.getHttpMethod(),
                auditLog.getRequestPath(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCorrelationId(),
                auditLog.getDetails());
    }

    private AuditEventType classify(String path) {
        if (path.startsWith("/access-control")) {
            return AuditEventType.SCAN;
        }
        if (path.startsWith("/sales")) {
            return path.matches(".*/(refund|cancel|payments)$")
                    ? AuditEventType.STATUS_CHANGE
                    : AuditEventType.SALE;
        }
        if (path.startsWith("/reservations")) {
            return path.matches(".*/(confirm|cancel)$")
                    ? AuditEventType.STATUS_CHANGE
                    : AuditEventType.RESERVATION;
        }
        if (path.matches(".*/(activate|deactivate|cancel|finish)$")) {
            return AuditEventType.STATUS_CHANGE;
        }
        return AuditEventType.CRUD;
    }

    private String entityType(String path) {
        String[] parts = path.split("/");
        return parts.length > 1 && !parts[1].isBlank()
                ? truncate(parts[1].toUpperCase(Locale.ROOT), 80)
                : null;
    }

    private String entityId(String path) {
        Matcher matcher = ENTITY_ID.matcher(path);
        String value = null;
        while (matcher.find()) {
            value = matcher.group(1);
        }
        return value;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? null
                : normalize(authentication.getName());
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(
                CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return value == null
                ? UUID.randomUUID().toString()
                : value.toString();
    }

    private String header(
            HttpServletRequest request,
            String name,
            int maximumLength) {
        return truncate(request.getHeader(name), maximumLength);
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : truncate(value.trim().toLowerCase(Locale.ROOT), 160);
    }

    private String truncate(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }
}
