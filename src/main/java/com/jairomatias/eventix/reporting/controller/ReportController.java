package com.jairomatias.eventix.reporting.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.service.ReportExportService;
import com.jairomatias.eventix.reporting.service.ReportService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.audit.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
@Controller
@RequestMapping("/reports")
public class ReportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;
    private final ReportExportService exportService;
    private final AuditService auditService;

    public ReportController(
            ReportService reportService,
            ReportExportService exportService,
            AuditService auditService) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.auditService = auditService;
    }

    @GetMapping
    public String index(
            @ModelAttribute("filter") ReportFilter filter,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        Long organizerScope = organizerScope(principal);
        ReportDataset report = reportService.generate(filter, organizerScope);
        applyNormalizedDates(filter, report);
        model.addAttribute("report", report);
        model.addAttribute("events", reportService.findEvents(organizerScope));
        model.addAttribute("categories", reportService.findCategories());
        model.addAttribute("isAdministrator", isAdministrator(principal));
        if (isAdministrator(principal)) {
            model.addAttribute("organizers", reportService.findOrganizers());
        }
        return "reports/index";
    }

    @GetMapping("/export/{format}")
    public ResponseEntity<byte[]> export(
            @PathVariable String format,
            @ModelAttribute ReportFilter filter,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        ReportDataset report = reportService.generate(
                filter,
                organizerScope(principal));
        ResponseEntity<byte[]> response = switch (format.toLowerCase()) {
            case "csv" -> download(
                    exportService.toCsv(report),
                    "eventix-report.csv",
                    new MediaType("text", "csv", StandardCharsets.UTF_8));
            case "xlsx" -> download(
                    exportService.toXlsx(report),
                    "eventix-report.xlsx",
                    XLSX);
            case "pdf" -> download(
                    exportService.toPdf(report),
                    "eventix-report.pdf",
                    MediaType.APPLICATION_PDF);
            default -> throw new BusinessRuleException(
                    "El formato de exportación no es válido.");
        };
        auditService.recordExport(
                format,
                principal.getUsername(),
                request);
        return response;
    }

    private ResponseEntity<byte[]> download(
            byte[] content,
            String filename,
            MediaType mediaType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content);
    }

    private Long organizerScope(UserPrincipal principal) {
        return "ORGANIZER".equals(principal.getRoleName())
                ? principal.getId()
                : null;
    }

    private boolean isAdministrator(UserPrincipal principal) {
        return "ADMINISTRATOR".equals(principal.getRoleName());
    }

    private void applyNormalizedDates(
            ReportFilter filter,
            ReportDataset report) {
        filter.setFrom(report.from());
        filter.setTo(report.to());
        if (filter.getTo() == null) {
            filter.setTo(LocalDate.now());
        }
    }
}
