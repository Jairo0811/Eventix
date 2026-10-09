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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.venue.dto.SeatHoldResult;
import com.jairomatias.eventix.venue.dto.SeatSelectionForm;
import com.jairomatias.eventix.venue.service.EventSeatInventoryService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/my/checkout/events/{eventId}/seats")
public class CustomerSeatSelectionController {

    private final EventSeatInventoryService inventoryService;

    public CustomerSeatSelectionController(
            EventSeatInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String select(
            @PathVariable Long eventId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("seatSelectionForm")) {
            model.addAttribute("seatSelectionForm", new SeatSelectionForm());
        }
        model.addAttribute("eventId", eventId);
        model.addAttribute("inventory", inventoryService.getInventory(eventId));
        inventoryService.getActiveHold(eventId, authentication.getName())
                .ifPresent(hold -> addHoldModelAttributes(hold, model));
        return "checkout/seats";
    }

    @PostMapping
    public String hold(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("seatSelectionForm") SeatSelectionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareModel(eventId, authentication, model);
            return "checkout/seats";
        }

        try {
            SeatHoldResult hold = inventoryService.holdSeats(
                    eventId,
                    form.getSeatIds(),
                    authentication.getName());
            addHoldFlashAttributes(hold, redirectAttributes, "Asientos retenidos durante 10 minutos.");
            return "redirect:/my/checkout/events/" + eventId + "/seats";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("seat.hold", exception.getMessage());
            prepareModel(eventId, authentication, model);
            return "checkout/seats";
        }
    }

    @PostMapping("/best-available")
    public String holdBestAvailable(
            @PathVariable Long eventId,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "false") boolean accessibilityRequired,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            SeatHoldResult hold = inventoryService.holdBestAvailableSeats(
                    eventId,
                    quantity,
                    accessibilityRequired,
                    authentication.getName());
            addHoldFlashAttributes(
                    hold,
                    redirectAttributes,
                    "Eventix encontró y retuvo los mejores asientos contiguos disponibles.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/my/checkout/events/" + eventId + "/seats";
    }

    @PostMapping("/release")
    public String release(
            @PathVariable Long eventId,
            @RequestParam String holdToken,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        inventoryService.releaseHold(
                eventId,
                holdToken,
                authentication.getName());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Los asientos retenidos fueron liberados.");
        return "redirect:/my/checkout/events/" + eventId + "/seats";
    }

    private void prepareModel(
            Long eventId,
            Authentication authentication,
            Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("inventory", inventoryService.getInventory(eventId));
        inventoryService.getActiveHold(eventId, authentication.getName())
                .ifPresent(hold -> addHoldModelAttributes(hold, model));
    }

    private void addHoldModelAttributes(SeatHoldResult hold, Model model) {
        model.addAttribute("holdToken", hold.holdToken());
        model.addAttribute("holdExpiresAt", hold.expiresAt());
        model.addAttribute("heldSeatIds", hold.seatIds());
    }

    private void addHoldFlashAttributes(
            SeatHoldResult hold,
            RedirectAttributes redirectAttributes,
            String successMessage) {
        redirectAttributes.addFlashAttribute("holdToken", hold.holdToken());
        redirectAttributes.addFlashAttribute("holdExpiresAt", hold.expiresAt());
        redirectAttributes.addFlashAttribute("heldSeatIds", hold.seatIds());
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
    }
}
