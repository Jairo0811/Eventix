package com.jairomatias.eventix.sale.controller;

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

import com.jairomatias.eventix.event.dto.EventDetailsView;
import com.jairomatias.eventix.event.service.EventService;
import com.jairomatias.eventix.sale.dto.TicketTypeForm;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.sale.service.TicketTypeService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/events/{eventId}/ticket-types")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;
    private final EventService eventService;

    public TicketTypeController(
            TicketTypeService ticketTypeService,
            EventService eventService) {
        this.ticketTypeService = ticketTypeService;
        this.eventService = eventService;
    }

    @ModelAttribute("ticketTypeCategories")
    public TicketTypeCategory[] ticketTypeCategories() {
        return TicketTypeCategory.values();
    }

    @GetMapping
    public String list(
            @PathVariable Long eventId,
            Authentication authentication,
            Model model) {
        EventDetailsView event = eventService.findById(
                eventId,
                authentication.getName());
        model.addAttribute("event", event);
        model.addAttribute(
                "ticketTypes",
                ticketTypeService.findByEvent(
                        eventId,
                        authentication.getName()));
        return "ticket-types/list";
    }

    @GetMapping("/new")
    public String createForm(
            @PathVariable Long eventId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("ticketTypeForm")) {
            model.addAttribute(
                    "ticketTypeForm",
                    ticketTypeService.getCreateForm(
                            eventId,
                            authentication.getName()));
        }
        prepareFormModel(
                eventId,
                null,
                "create",
                authentication,
                model);
        return "ticket-types/form";
    }

    @PostMapping
    public String create(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("ticketTypeForm") TicketTypeForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(
                    eventId,
                    null,
                    "create",
                    authentication,
                    model);
            return "ticket-types/form";
        }
        try {
            ticketTypeService.create(
                    eventId,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tipo de entrada creado correctamente.");
            return "redirect:/events/" + eventId + "/ticket-types";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            prepareFormModel(
                    eventId,
                    null,
                    "create",
                    authentication,
                    model);
            return "ticket-types/form";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("ticketType.create", exception.getMessage());
            prepareFormModel(
                    eventId,
                    null,
                    "create",
                    authentication,
                    model);
            return "ticket-types/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long eventId,
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("ticketTypeForm")) {
            model.addAttribute(
                    "ticketTypeForm",
                    ticketTypeService.getUpdateForm(
                            id,
                            authentication.getName()));
        }
        prepareFormModel(
                eventId,
                id,
                "edit",
                authentication,
                model);
        return "ticket-types/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long eventId,
            @PathVariable Long id,
            @Valid @ModelAttribute("ticketTypeForm") TicketTypeForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(
                    eventId,
                    id,
                    "edit",
                    authentication,
                    model);
            return "ticket-types/form";
        }
        try {
            ticketTypeService.update(
                    id,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tipo de entrada actualizado correctamente.");
            return "redirect:/events/" + eventId + "/ticket-types";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            prepareFormModel(
                    eventId,
                    id,
                    "edit",
                    authentication,
                    model);
            return "ticket-types/form";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("ticketType.update", exception.getMessage());
            prepareFormModel(
                    eventId,
                    id,
                    "edit",
                    authentication,
                    model);
            return "ticket-types/form";
        }
    }

    private void prepareFormModel(
            Long eventId,
            Long ticketTypeId,
            String mode,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "event",
                eventService.findById(eventId, authentication.getName()));
        model.addAttribute("formMode", mode);
        model.addAttribute("ticketTypeId", ticketTypeId);
    }
}
