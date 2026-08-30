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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.eligibility.dto.SchoolAlumniBenefitConfiguration;
import com.jairomatias.eventix.eligibility.dto.SchoolAlumniBenefitForm;
import com.jairomatias.eventix.eligibility.service.SchoolAlumniBenefitService;
import com.jairomatias.eventix.eligibility.service.SchoolPromotionManagementService;
import com.jairomatias.eventix.event.service.EventService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/events/{eventId}/school-alumni-benefit")
public class SchoolAlumniBenefitController {

    private final SchoolAlumniBenefitService alumniBenefitService;
    private final SchoolPromotionManagementService promotionManagementService;
    private final EventService eventService;

    public SchoolAlumniBenefitController(
            SchoolAlumniBenefitService alumniBenefitService,
            SchoolPromotionManagementService promotionManagementService,
            EventService eventService) {
        this.alumniBenefitService = alumniBenefitService;
        this.promotionManagementService = promotionManagementService;
        this.eventService = eventService;
    }

    @GetMapping
    public String form(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "false") boolean setup,
            Authentication authentication,
            Model model) {
        requireSchoolPromotionEvent(eventId);
        if (!model.containsAttribute("alumniBenefitForm")) {
            SchoolAlumniBenefitConfiguration configuration =
                    alumniBenefitService.getConfiguration(
                            eventId,
                            authentication.getName());
            SchoolAlumniBenefitForm form = SchoolAlumniBenefitForm.from(configuration);
            if (setup && configuration.schoolPromotionId() == null) {
                form.setEnabled(true);
            }
            model.addAttribute("alumniBenefitForm", form);
        }
        prepareModel(eventId, authentication, model);
        return "eligibility/school-alumni-benefit";
    }

    @PostMapping
    public String save(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("alumniBenefitForm") SchoolAlumniBenefitForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        requireSchoolPromotionEvent(eventId);
        if (bindingResult.hasErrors()) {
            prepareModel(eventId, authentication, model);
            return "eligibility/school-alumni-benefit";
        }

        try {
            alumniBenefitService.configure(
                    eventId,
                    Boolean.TRUE.equals(form.getEnabled()),
                    form.getSchoolPromotionId(),
                    form.getDiscountType(),
                    form.getDiscountValue(),
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    Boolean.TRUE.equals(form.getEnabled())
                            ? "Descuento exclusivo para egresados configurado correctamente."
                            : "Descuento para egresados desactivado correctamente.");
            return "redirect:/events/" + eventId;
        } catch (BusinessRuleException exception) {
            bindingResult.reject("schoolAlumniBenefit", exception.getMessage());
            prepareModel(eventId, authentication, model);
            return "eligibility/school-alumni-benefit";
        }
    }

    private void prepareModel(
            Long eventId,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "event",
                eventService.findById(eventId, authentication.getName()));
        model.addAttribute(
                "schoolPromotions",
                promotionManagementService.listActivePromotions());
    }

    private void requireSchoolPromotionEvent(Long eventId) {
        if (!alumniBenefitService.isSchoolPromotionEvent(eventId)) {
            throw new BusinessRuleException(
                    "Este beneficio solo está disponible para eventos de Promoción escolar.");
        }
    }
}
