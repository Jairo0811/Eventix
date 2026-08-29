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

import com.jairomatias.eventix.eligibility.dto.EligibilityBenefitForm;
import com.jairomatias.eventix.eligibility.dto.EligibilityGroupForm;
import com.jairomatias.eventix.eligibility.dto.EligibilityGroupView;
import com.jairomatias.eventix.eligibility.dto.EligibilityMembershipForm;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.service.EligibilityBenefitManagementService;
import com.jairomatias.eventix.eligibility.service.EligibilityGroupManagementService;
import com.jairomatias.eventix.eligibility.service.EligibilityMembershipManagementService;
import com.jairomatias.eventix.eligibility.service.SchoolPromotionManagementService;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/events/{eventId}/eligibility")
public class EligibilityGroupController {

    private final EligibilityGroupManagementService groupService;
    private final EligibilityMembershipManagementService membershipService;
    private final EligibilityBenefitManagementService benefitService;
    private final SchoolPromotionManagementService schoolPromotionManagementService;
    private final TicketTypeRepository ticketTypeRepository;

    public EligibilityGroupController(
            EligibilityGroupManagementService groupService,
            EligibilityMembershipManagementService membershipService,
            EligibilityBenefitManagementService benefitService,
            SchoolPromotionManagementService schoolPromotionManagementService,
            TicketTypeRepository ticketTypeRepository) {
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.benefitService = benefitService;
        this.schoolPromotionManagementService = schoolPromotionManagementService;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @GetMapping
    public String index(@PathVariable Long eventId, Authentication authentication, Model model) {
        Long actorId = principal(authentication).getId();
        model.addAttribute("eventId", eventId);
        model.addAttribute("groups", groupService.list(eventId, actorId));
        model.addAttribute("groupTypes", EligibilityGroupType.values());
        model.addAttribute("schoolPromotions", schoolPromotionManagementService.listActivePromotions());
        if (!model.containsAttribute("groupForm")) {
            model.addAttribute("groupForm", new EligibilityGroupForm("", EligibilityGroupType.CUSTOM, null, null));
        }
        return "eligibility/groups";
    }

    @GetMapping("/groups/{groupId}")
    public String detail(
            @PathVariable Long eventId,
            @PathVariable Long groupId,
            Authentication authentication,
            Model model) {
        Long actorId = principal(authentication).getId();
        EligibilityGroupView group = groupService.list(eventId, actorId).stream()
                .filter(item -> item.id().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo en este evento."));
        model.addAttribute("eventId", eventId);
        model.addAttribute("group", group);
        model.addAttribute("memberships", membershipService.list(groupId, actorId));
        model.addAttribute("benefits", benefitService.list(groupId, actorId));
        model.addAttribute("benefitTypes", EligibilityBenefitType.values());
        model.addAttribute("ticketTypes", ticketTypeRepository.findAllByEvent_IdAndActiveTrueOrderByNameAsc(eventId));
        if (!model.containsAttribute("membershipForm")) {
            model.addAttribute("membershipForm", new EligibilityMembershipForm(""));
        }
        if (!model.containsAttribute("benefitForm")) {
            model.addAttribute("benefitForm", new EligibilityBenefitForm(
                    EligibilityBenefitType.PRIORITY_ACCESS, null, null, null, null, null));
        }
        return "eligibility/group-detail";
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
            groupService.create(eventId, form, principal(authentication).getId());
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
            groupService.update(groupId, form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Grupo actualizado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(eventId);
    }

    @PostMapping("/groups/{groupId}/activate")
    public String activate(@PathVariable Long eventId, @PathVariable Long groupId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        return changeGroupStatus(eventId, groupId, true, authentication, redirectAttributes);
    }

    @PostMapping("/groups/{groupId}/deactivate")
    public String deactivate(@PathVariable Long eventId, @PathVariable Long groupId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        return changeGroupStatus(eventId, groupId, false, authentication, redirectAttributes);
    }

    @PostMapping("/groups/{groupId}/members")
    public String addMember(@PathVariable Long eventId, @PathVariable Long groupId,
            @Valid @ModelAttribute("membershipForm") EligibilityMembershipForm form,
            BindingResult bindingResult, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return redirectGroup(eventId, groupId);
        }
        try {
            membershipService.addVerified(groupId, form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Miembro verificado agregado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirectGroup(eventId, groupId);
    }

    @PostMapping("/groups/{groupId}/members/{membershipId}/revoke")
    public String revokeMember(@PathVariable Long eventId, @PathVariable Long groupId,
            @PathVariable Long membershipId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            membershipService.revoke(membershipId, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Membresía revocada correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirectGroup(eventId, groupId);
    }

    @PostMapping("/groups/{groupId}/benefits")
    public String addBenefit(@PathVariable Long eventId, @PathVariable Long groupId,
            @Valid @ModelAttribute("benefitForm") EligibilityBenefitForm form,
            BindingResult bindingResult, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return redirectGroup(eventId, groupId);
        }
        try {
            benefitService.create(groupId, form, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage", "Beneficio configurado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirectGroup(eventId, groupId);
    }

    @PostMapping("/groups/{groupId}/benefits/{benefitId}/activate")
    public String activateBenefit(@PathVariable Long eventId, @PathVariable Long groupId,
            @PathVariable Long benefitId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeBenefitStatus(eventId, groupId, benefitId, true, authentication, redirectAttributes);
    }

    @PostMapping("/groups/{groupId}/benefits/{benefitId}/deactivate")
    public String deactivateBenefit(@PathVariable Long eventId, @PathVariable Long groupId,
            @PathVariable Long benefitId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return changeBenefitStatus(eventId, groupId, benefitId, false, authentication, redirectAttributes);
    }

    private String changeGroupStatus(Long eventId, Long groupId, boolean active,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            groupService.setActive(groupId, active, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    active ? "Grupo activado correctamente." : "Grupo desactivado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirect(eventId);
    }

    private String changeBenefitStatus(Long eventId, Long groupId, Long benefitId, boolean active,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            benefitService.setActive(benefitId, active, principal(authentication).getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    active ? "Beneficio activado correctamente." : "Beneficio desactivado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirectGroup(eventId, groupId);
    }

    private UserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessRuleException("Debes iniciar sesión para administrar la elegibilidad.");
        }
        return principal;
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "Revisa los datos ingresados."
                : bindingResult.getAllErrors().getFirst().getDefaultMessage();
    }

    private String redirect(Long eventId) {
        return "redirect:/events/" + eventId + "/eligibility";
    }

    private String redirectGroup(Long eventId, Long groupId) {
        return "redirect:/events/" + eventId + "/eligibility/groups/" + groupId;
    }
}
