package com.jairomatias.eventix.ticket.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.ticket.dto.ScanForm;
import com.jairomatias.eventix.ticket.dto.ScanResultView;
import com.jairomatias.eventix.ticket.service.AccessControlService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/access-control")
public class AccessControlController {

    private static final int PAGE_SIZE = 20;

    private final AccessControlService accessControlService;

    public AccessControlController(
            AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("scanForm")) {
            model.addAttribute("scanForm", new ScanForm());
        }
        model.addAttribute(
                "summary",
                accessControlService.getSummary(
                        eventId,
                        authentication.getName()));
        model.addAttribute(
                "attempts",
                accessControlService.findAttempts(
                        eventId,
                        authentication.getName(),
                        PageRequest.of(
                                Math.max(page, 0),
                                PAGE_SIZE,
                                Sort.by("occurredAt").descending())));
        model.addAttribute(
                "events",
                accessControlService.findVisibleEvents(
                        authentication.getName()));
        model.addAttribute("selectedEventId", eventId);
        return "access/index";
    }

    @PostMapping("/scan")
    public String scan(
            @Valid @ModelAttribute("scanForm") ScanForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().getFirst()
                            .getDefaultMessage());
            return "redirect:/access-control";
        }
        ScanResultView result = accessControlService.scan(
                form,
                authentication.getName(),
                request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("scanResult", result);
        redirectAttributes.addFlashAttribute(
                result.accepted() ? "successMessage" : "errorMessage",
                result.message());
        return "redirect:/access-control";
    }
}
