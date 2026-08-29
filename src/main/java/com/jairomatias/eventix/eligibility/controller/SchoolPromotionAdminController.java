package com.jairomatias.eventix.eligibility.controller;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.eligibility.dto.RosterImportResult;
import com.jairomatias.eventix.eligibility.dto.SchoolInstitutionForm;
import com.jairomatias.eventix.eligibility.dto.SchoolPromotionForm;
import com.jairomatias.eventix.eligibility.service.EligibilityManualReviewService;
import com.jairomatias.eventix.eligibility.service.SchoolPromotionManagementService;
import com.jairomatias.eventix.eligibility.service.SchoolRosterImportService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/school-promotions")
public class SchoolPromotionAdminController {

    private final SchoolPromotionManagementService managementService;
    private final SchoolRosterImportService rosterImportService;
    private final EligibilityManualReviewService manualReviewService;

    public SchoolPromotionAdminController(
            SchoolPromotionManagementService managementService,
            SchoolRosterImportService rosterImportService,
            EligibilityManualReviewService manualReviewService) {
        this.managementService = managementService;
        this.rosterImportService = rosterImportService;
        this.manualReviewService = manualReviewService;
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        Long actorId = principal(authentication).getId();
        model.addAttribute("institutions", managementService.listInstitutions(actorId));
        model.addAttribute("promotions", managementService.listPromotions(actorId));
        if (!model.containsAttribute("institutionForm")) {
            model.addAttribute("institutionForm", new SchoolInstitutionForm("", ""));
        }
        if (!model.containsAttribute("promotionForm")) {
            model.addAttribute("promotionForm", new SchoolPromotionForm(null, "", java.time.Year.now().getValue()));
        }
        return "school-promotions/index";
    }

    @PostMapping("/institutions")
    public String createInstitution(
            @Valid @ModelAttribute("institutionForm") SchoolInstitutionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return error(redirectAttributes, firstError(bindingResult), "/school-promotions");
        }
        try {
            managementService.createInstitution(form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Institución creada correctamente.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/school-promotions";
    }

    @PostMapping("/promotions")
    public String createPromotion(
            @Valid @ModelAttribute("promotionForm") SchoolPromotionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return error(redirectAttributes, firstError(bindingResult), "/school-promotions");
        }
        try {
            Long id = managementService.createPromotion(form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Promoción escolar creada correctamente.");
            return "redirect:/school-promotions/promotions/" + id;
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/school-promotions";
        }
    }

    @GetMapping("/promotions/{promotionId}")
    public String detail(@PathVariable Long promotionId, Authentication authentication, Model model) {
        Long actorId = principal(authentication).getId();
        model.addAttribute("promotion", managementService.getPromotion(promotionId, actorId));
        model.addAttribute("members", managementService.listMembers(promotionId, actorId));
        model.addAttribute("imports", managementService.listImports(promotionId, actorId));
        return "school-promotions/detail";
    }

    @PostMapping("/promotions/{promotionId}/roster")
    public String importRoster(
            @PathVariable Long promotionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceName", required = false) String sourceName,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Long actorId = principal(authentication).getId();
        managementService.getPromotion(promotionId, actorId);
        String source = sourceName == null || sourceName.isBlank() ? file.getOriginalFilename() : sourceName;
        try {
            RosterImportResult result = rosterImportService.importCsv(
                    promotionId, actorId, source, file.getBytes());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Padrón importado: " + result.acceptedRows() + " aceptados y "
                            + result.rejectedRows() + " rechazados.");
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo leer el archivo seleccionado.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/school-promotions/promotions/" + promotionId;
    }

    @GetMapping("/verifications")
    public String verifications(Authentication authentication, Model model) {
        model.addAttribute("verifications",
                managementService.listVerifications(principal(authentication).getId()));
        return "school-promotions/verifications";
    }

    @PostMapping("/verifications/{verificationId}/approve")
    public String approve(@PathVariable Long verificationId, @RequestParam String reason,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        return review(verificationId, reason, "approve", authentication, redirectAttributes);
    }

    @PostMapping("/verifications/{verificationId}/reject")
    public String reject(@PathVariable Long verificationId, @RequestParam String reason,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        return review(verificationId, reason, "reject", authentication, redirectAttributes);
    }

    @PostMapping("/verifications/{verificationId}/revoke")
    public String revoke(@PathVariable Long verificationId, @RequestParam String reason,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        return review(verificationId, reason, "revoke", authentication, redirectAttributes);
    }

    private String review(Long verificationId, String reason, String action,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Long actorId = principal(authentication).getId();
            switch (action) {
                case "approve" -> manualReviewService.approve(verificationId, actorId, reason);
                case "reject" -> manualReviewService.reject(verificationId, actorId, reason);
                case "revoke" -> manualReviewService.revoke(verificationId, actorId, reason);
                default -> throw new IllegalArgumentException("Acción de revisión no válida.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Verificación actualizada correctamente.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/school-promotions/verifications";
    }

    private UserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessRuleException("Debes iniciar sesión.");
        }
        return principal;
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "Revisa los datos ingresados."
                : bindingResult.getAllErrors().getFirst().getDefaultMessage();
    }

    private String error(RedirectAttributes redirectAttributes, String message, String path) {
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:" + path;
    }
}
