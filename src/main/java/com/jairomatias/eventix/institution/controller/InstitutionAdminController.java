package com.jairomatias.eventix.institution.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.institution.service.InstitutionAccountService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Controller
@RequestMapping("/institutions/admin")
public class InstitutionAdminController {

    private final InstitutionAccountService accountService;

    public InstitutionAdminController(InstitutionAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/requests")
    public String requests(Authentication authentication, Model model) {
        model.addAttribute(
                "requests",
                accountService.listPendingForAdministrator(principal(authentication).getId()));
        return "institutions/admin/requests";
    }

    @PostMapping("/{institutionId}/approve")
    public String approve(
            @PathVariable Long institutionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeStatus(institutionId, "approve", authentication, redirectAttributes);
    }

    @PostMapping("/{institutionId}/reject")
    public String reject(
            @PathVariable Long institutionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeStatus(institutionId, "reject", authentication, redirectAttributes);
    }

    @PostMapping("/{institutionId}/suspend")
    public String suspend(
            @PathVariable Long institutionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeStatus(institutionId, "suspend", authentication, redirectAttributes);
    }

    private String changeStatus(
            Long institutionId,
            String action,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long actorId = principal(authentication).getId();
            switch (action) {
                case "approve" -> accountService.approve(institutionId, actorId);
                case "reject" -> accountService.reject(institutionId, actorId);
                case "suspend" -> accountService.suspend(institutionId, actorId);
                default -> throw new IllegalArgumentException("Acción institucional no válida.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Estado institucional actualizado.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/institutions/admin/requests";
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
