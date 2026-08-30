package com.jairomatias.eventix.institution.controller;

import java.io.IOException;
import java.time.Year;
import java.util.List;

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
import com.jairomatias.eventix.eligibility.dto.SchoolPromotionForm;
import com.jairomatias.eventix.eligibility.service.SchoolPromotionManagementService;
import com.jairomatias.eventix.eligibility.service.SchoolRosterImportService;
import com.jairomatias.eventix.institution.dto.InstitutionMemberForm;
import com.jairomatias.eventix.institution.dto.InstitutionRegistrationForm;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.service.InstitutionAccountService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/institutions")
public class InstitutionAccountController {

    private final InstitutionAccountService accountService;
    private final SchoolPromotionManagementService promotionService;
    private final SchoolRosterImportService rosterImportService;

    public InstitutionAccountController(
            InstitutionAccountService accountService,
            SchoolPromotionManagementService promotionService,
            SchoolRosterImportService rosterImportService) {
        this.accountService = accountService;
        this.promotionService = promotionService;
        this.rosterImportService = rosterImportService;
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        Long actorId = principal(authentication).getId();
        model.addAttribute("memberships", accountService.listForUser(actorId));
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new InstitutionRegistrationForm("", ""));
        }
        return "institutions/index";
    }

    @PostMapping
    public String register(
            @Valid @ModelAttribute("registrationForm") InstitutionRegistrationForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/institutions";
        }
        try {
            Long id = accountService.register(form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Centro registrado. Eventix debe verificarlo antes de habilitar su operación.");
            return "redirect:/institutions/" + id;
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/institutions";
        }
    }

    @GetMapping("/{institutionId}")
    public String dashboard(
            @PathVariable Long institutionId,
            Authentication authentication,
            Model model) {
        Long actorId = principal(authentication).getId();
        var dashboard = accountService.getDashboard(institutionId, actorId);
        model.addAttribute("institution", dashboard);
        model.addAttribute("members", accountService.listMembers(institutionId, actorId));
        model.addAttribute("roles", List.of(
                InstitutionMembershipRole.ADMIN,
                InstitutionMembershipRole.EVENT_MANAGER,
                InstitutionMembershipRole.ROSTER_MANAGER,
                InstitutionMembershipRole.FINANCE));
        model.addAttribute("memberForm", new InstitutionMemberForm("", InstitutionMembershipRole.EVENT_MANAGER));
        model.addAttribute("promotionForm", new SchoolPromotionForm(
                institutionId,
                "",
                Year.now().getValue()));
        model.addAttribute(
                "promotions",
                promotionService.listPromotionsForInstitution(institutionId, actorId));
        return "institutions/dashboard";
    }

    @PostMapping("/{institutionId}/members")
    public String addMember(
            @PathVariable Long institutionId,
            @Valid @ModelAttribute("memberForm") InstitutionMemberForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return redirect(institutionId);
        }
        try {
            accountService.addMember(institutionId, form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Miembro agregado correctamente.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(institutionId);
    }

    @PostMapping("/{institutionId}/members/{membershipId}/role")
    public String changeRole(
            @PathVariable Long institutionId,
            @PathVariable Long membershipId,
            @RequestParam InstitutionMembershipRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.changeMemberRole(
                    institutionId,
                    membershipId,
                    role,
                    principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Rol institucional actualizado.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(institutionId);
    }

    @PostMapping("/{institutionId}/members/{membershipId}/status")
    public String changeMembershipStatus(
            @PathVariable Long institutionId,
            @PathVariable Long membershipId,
            @RequestParam boolean active,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.setMemberActive(
                    institutionId,
                    membershipId,
                    active,
                    principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Estado de membresía actualizado.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(institutionId);
    }

    @PostMapping("/{institutionId}/promotions")
    public String createPromotion(
            @PathVariable Long institutionId,
            @RequestParam String name,
            @RequestParam int graduationYear,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            promotionService.createPromotion(
                    new SchoolPromotionForm(institutionId, name, graduationYear),
                    principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Promoción creada correctamente.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(institutionId);
    }

    @PostMapping("/{institutionId}/promotions/{promotionId}/roster")
    public String importRoster(
            @PathVariable Long institutionId,
            @PathVariable Long promotionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceName", required = false) String sourceName,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Long actorId = principal(authentication).getId();
        try {
            var promotion = promotionService.getPromotion(promotionId, actorId);
            if (!promotion.institutionId().equals(institutionId)) {
                throw new BusinessRuleException("La promoción no pertenece a este centro educativo.");
            }
            String source = sourceName == null || sourceName.isBlank()
                    ? file.getOriginalFilename()
                    : sourceName;
            RosterImportResult result = rosterImportService.importCsv(
                    promotionId,
                    actorId,
                    source,
                    file.getBytes());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Padrón importado: " + result.acceptedRows() + " aceptados y "
                            + result.rejectedRows() + " rechazados.");
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo leer el archivo seleccionado.");
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(institutionId);
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }

    private String redirect(Long institutionId) {
        return "redirect:/institutions/" + institutionId;
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Datos inválidos." : error.getDefaultMessage())
                .orElse("Datos inválidos.");
    }
}
