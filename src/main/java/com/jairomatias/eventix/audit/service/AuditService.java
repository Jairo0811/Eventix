package com.jairomatias.eventix.audit.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.audit.dto.AuditLogView;
import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditOutcome;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {

    void recordHttpMutation(
            HttpServletRequest request,
            int status,
            Exception exception);

    void recordAuthentication(
            AuditEventType eventType,
            AuditOutcome outcome,
            String username,
            HttpServletRequest request,
            String details);

    void recordExport(
            String format,
            String username,
            HttpServletRequest request);

    void recordError(
            Throwable exception,
            HttpServletRequest request);

    Page<AuditLogView> findAll(
            String term,
            AuditEventType eventType,
            AuditOutcome outcome,
            LocalDate from,
            LocalDate to,
            Pageable pageable);
}
