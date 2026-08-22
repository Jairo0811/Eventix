package com.jairomatias.eventix.eligibility.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.eligibility.dto.EligibilityGroupForm;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.service.EligibilityGroupManagementService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/events/{eventId}/eligibility")
public class EligibilityGroupController {

    private final EligibilityGroupManagementService service;

    public EligibilityGroupController(EligibilityGroupManagementService service) {
        this.service = service;
    }

    @GetMapping
    public String index(@PathVariable Long eventId, Authentication authentication, Model model) {
        Long actorId = principal(authentication).getId();
        model.addAttribute("eventId", eventId);
        model.addAttribute("groups", service.list(eventId, actorId));
        model.addAttribute("groupTypes", EligibilityGroupType.values());
        if (!model.containsAttribute("groupForm")) {
            model.addAttribute("groupForm", new EligibilityGroupForm("", EligibilityGroupType.CUSTOM, null));
        }
        return "eligibility/groups";
    }

    @PostMapping("/groups")
    public String create(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("groupForm") EligibilityGroupForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return redirect(eventId);
        }
        try {
            service.create(eventId, form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Grupo de elegibilidad creado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(eventId);
    }

    @PostMapping("/groups/{groupId}")
    public String update(
            @PathVariable Long eventId,
            @PathVariable Long groupId,
            @Valid EligibilityGroupForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return redirect(eventId);
        }
        try {
            service.update(groupId, form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Grupo actualizado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(eventId);
    }

    @PostMapping("/groups/{groupId}/activate")
    public String activate(
            @PathVariable Long eventId,
            @PathVariable Long groupId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeStatus(eventId, groupId, true, authentication, redirectAttributes);
    }

    @PostMapping("/groups/{groupId}/deactivate")
    public String deactivate(
            @PathVariable Long eventId,
            @PathVariable Long groupId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeStatus(eventId, groupId, false, authentication, redirectAttributes);
    }

    private String changeStatus(
            Long eventId,
            Long groupId,
            boolean active,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            service.setActive(groupId, active, principal(authentication).getId());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    active ? "Grupo activado correctamente." : "Grupo desactivado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(eventId);
    }

    private UserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessRuleException("Debes iniciar sesión para administrar la elegibilidad.");
        }
        return principal;
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "Revisa los datos del grupo."
                : bindingResult.getAllErrors().getFirst().getDefaultMessage();
    }

    private String redirect(Long eventId) {
        return "redirect:/events/" + eventId + "/eligibility";
    }
}
