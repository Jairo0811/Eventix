package com.jairomatias.eventix.settlement.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import com.jairomatias.eventix.settlement.dto.SettlementActionForm;
import com.jairomatias.eventix.settlement.dto.SettlementCreateForm;
import com.jairomatias.eventix.settlement.entity.SettlementStatus;
import com.jairomatias.eventix.settlement.service.OrganizerSettlementService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/settlements")
public class OrganizerSettlementController {

    private static final int PAGE_SIZE = 12;

    private final OrganizerSettlementService settlementService;

    public OrganizerSettlementController(
            OrganizerSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @ModelAttribute("settlementStatuses")
    public SettlementStatus[] settlementStatuses() {
        return SettlementStatus.values();
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("createdAt").descending());
        model.addAttribute(
                "settlements",
                settlementService.findAll(
                        status,
                        authentication.getName(),
                        pageable));
        model.addAttribute("selectedStatus", status);
        return "settlements/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("settlementForm")) {
            model.addAttribute(
                    "settlementForm",
                    settlementService.getCreateForm());
        }
        prepareCreateModel(model);
        return "settlements/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("settlementForm")
            SettlementCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareCreateModel(model);
            return "settlements/form";
        }
        try {
            Long id = settlementService.create(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Liquidación creada con sus movimientos financieros.");
            return "redirect:/settlements/" + id;
        } catch (BusinessRuleException exception) {
            bindingResult.reject("settlement.create", exception.getMessage());
            prepareCreateModel(model);
            return "settlements/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "settlement",
                settlementService.findById(id, authentication.getName()));
        model.addAttribute("settlementActionForm", new SettlementActionForm());
        return "settlements/detail";
    }

    @PostMapping("/{id}/processing")
    public String startProcessing(
            @PathVariable Long id,
            @Valid @ModelAttribute("settlementActionForm")
            SettlementActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executeAction(
                id,
                bindingResult,
                redirectAttributes,
                () -> settlementService.startProcessing(id, form),
                "Liquidación enviada a procesamiento.");
    }

    @PostMapping("/{id}/paid")
    public String markPaid(
            @PathVariable Long id,
            @Valid @ModelAttribute("settlementActionForm")
            SettlementActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executeAction(
                id,
                bindingResult,
                redirectAttributes,
                () -> settlementService.markPaid(id, form),
                "Liquidación marcada como pagada.");
    }

    @PostMapping("/{id}/failed")
    public String markFailed(
            @PathVariable Long id,
            @Valid @ModelAttribute("settlementActionForm")
            SettlementActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executeAction(
                id,
                bindingResult,
                redirectAttributes,
                () -> settlementService.markFailed(id, form),
                "Fallo de liquidación registrado.");
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @Valid @ModelAttribute("settlementActionForm")
            SettlementActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executeAction(
                id,
                bindingResult,
                redirectAttributes,
                () -> settlementService.cancel(id, form),
                "Liquidación cancelada; sus movimientos pueden reagruparse.");
    }

    private String executeAction(
            Long id,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Runnable action,
            String successMessage) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().getFirst()
                            .getDefaultMessage());
            return "redirect:/settlements/" + id;
        }
        try {
            action.run();
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    successMessage);
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/settlements/" + id;
    }

    private void prepareCreateModel(Model model) {
        model.addAttribute(
                "organizers",
                settlementService.findOrganizerOptions());
    }
}
