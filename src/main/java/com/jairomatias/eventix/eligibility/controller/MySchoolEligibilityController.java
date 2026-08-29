package com.jairomatias.eventix.eligibility.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.eligibility.dto.SchoolEligibilityResult;
import com.jairomatias.eventix.eligibility.service.SchoolEligibilityService;
import com.jairomatias.eventix.eligibility.service.SchoolPromotionManagementService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Controller
@RequestMapping("/my/eligibility/school")
public class MySchoolEligibilityController {

    private final SchoolEligibilityService schoolEligibilityService;
    private final SchoolPromotionManagementService managementService;

    public MySchoolEligibilityController(
            SchoolEligibilityService schoolEligibilityService,
            SchoolPromotionManagementService managementService) {
        this.schoolEligibilityService = schoolEligibilityService;
        this.managementService = managementService;
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        Long userId = principal(authentication).getId();
        model.addAttribute("promotions", managementService.listActivePromotions());
        model.addAttribute("verifications", managementService.listUserVerifications(userId));
        return "eligibility/my-school-verification";
    }

    @PostMapping
    public String verify(
            @RequestParam Long promotionId,
            @RequestParam String nationalId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            SchoolEligibilityResult result = schoolEligibilityService.verifyAndPersist(
                    principal(authentication).getId(), promotionId, nationalId);
            switch (result.status()) {
                case "VERIFIED" -> redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Identidad verificada. El nombre asociado a tu cédula aparece en el padrón de la promoción.");
                case "MANUAL_REVIEW" -> redirectAttributes.addFlashAttribute(
                        "warningMessage", "Esta verificación permanece pendiente de revisión administrativa.");
                case "IDENTITY_NOT_FOUND" -> redirectAttributes.addFlashAttribute(
                        "errorMessage", "No fue posible obtener una identidad válida para la cédula indicada.");
                case "PROVIDER_UNAVAILABLE" -> redirectAttributes.addFlashAttribute(
                        "errorMessage", "El servicio autorizado de identidad no está disponible en este momento.");
                case "AMBIGUOUS_MATCH" -> redirectAttributes.addFlashAttribute(
                        "warningMessage",
                        "El nombre asociado a la cédula aparece más de una vez en el padrón y requiere validación adicional.");
                case "NOT_FOUND" -> redirectAttributes.addFlashAttribute(
                        "errorMessage", "El nombre asociado a la cédula no aparece en el padrón de esa promoción.");
                default -> redirectAttributes.addFlashAttribute(
                        "errorMessage", "La verificación no puede aprobarse en su estado actual: " + result.status());
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/my/eligibility/school";
    }

    private UserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessRuleException("Debes iniciar sesión para verificar tu promoción.");
        }
        return principal;
    }
}
