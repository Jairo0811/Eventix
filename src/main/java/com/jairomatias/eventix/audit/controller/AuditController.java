package com.jairomatias.eventix.audit.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jairomatias.eventix.audit.dto.AuditLogView;
import com.jairomatias.eventix.audit.entity.AuditEventType;
import com.jairomatias.eventix.audit.entity.AuditOutcome;
import com.jairomatias.eventix.audit.service.AuditService;

@Controller
@RequestMapping("/audit")
public class AuditController {

    private static final int PAGE_SIZE = 25;

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("occurredAt").descending());
        Page<AuditLogView> entries = auditService.findAll(
                term,
                eventType,
                outcome,
                from,
                to,
                pageable);
        model.addAttribute("entries", entries);
        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("outcomes", AuditOutcome.values());
        model.addAttribute("term", term);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("selectedOutcome", outcome);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "audit/index";
    }
}
