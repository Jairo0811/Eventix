package com.jairomatias.eventix.venue.controller;

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

import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.venue.dto.EventVenueConfigurationForm;
import com.jairomatias.eventix.venue.service.EventSeatInventoryService;
import com.jairomatias.eventix.venue.service.EventVenueConfigurationService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/events/{eventId}/seating")
public class EventVenueConfigurationController {

    private final EventVenueConfigurationService configurationService;
    private final EventSeatInventoryService inventoryService;

    public EventVenueConfigurationController(
            EventVenueConfigurationService configurationService,
            EventSeatInventoryService inventoryService) {
        this.configurationService = configurationService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String form(
            @PathVariable Long eventId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("seatingForm")) {
            model.addAttribute(
                    "seatingForm",
                    configurationService.getForm(eventId, authentication.getName()));
        }
        prepareModel(eventId, model);
        return "events/seating";
    }

    @PostMapping
    public String configure(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("seatingForm") EventVenueConfigurationForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareModel(eventId, model);
            return "events/seating";
        }

        try {
            configurationService.configure(eventId, form, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Configuración de recinto y asientos actualizada.");
            return "redirect:/events/" + eventId + "/seating";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("seating.configure", exception.getMessage());
            prepareModel(eventId, model);
            return "events/seating";
        }
    }

    @PostMapping("/inventory")
    public String initializeInventory(
            @PathVariable Long eventId,
            RedirectAttributes redirectAttributes) {
        try {
            int created = inventoryService.initializeInventory(eventId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Inventario inicializado con " + created + " asientos.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/events/" + eventId + "/seating";
    }

    private void prepareModel(Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("venues", configurationService.getActiveVenues());
        model.addAttribute("seatingModes", EventSeatingMode.values());
        model.addAttribute("inventory", inventoryService.getInventory(eventId));
    }
}
