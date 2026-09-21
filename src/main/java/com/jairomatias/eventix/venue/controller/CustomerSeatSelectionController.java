package com.jairomatias.eventix.venue.controller;

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
            Model model) {
        if (!model.containsAttribute("seatSelectionForm")) {
            model.addAttribute("seatSelectionForm", new SeatSelectionForm());
        }
        model.addAttribute("eventId", eventId);
        model.addAttribute("inventory", inventoryService.getInventory(eventId));
        return "checkout/seats";
    }

    @PostMapping
    public String hold(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("seatSelectionForm") SeatSelectionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("eventId", eventId);
            model.addAttribute("inventory", inventoryService.getInventory(eventId));
            return "checkout/seats";
        }

        try {
            SeatHoldResult hold = inventoryService.holdSeats(
                    eventId,
                    form.getSeatIds());

            redirectAttributes.addFlashAttribute("holdToken", hold.holdToken());
            redirectAttributes.addFlashAttribute("holdExpiresAt", hold.expiresAt());
            redirectAttributes.addFlashAttribute("heldSeatIds", hold.seatIds());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Asientos retenidos durante 10 minutos.");

            return "redirect:/my/checkout/events/" + eventId + "/seats";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("seat.hold", exception.getMessage());
            model.addAttribute("eventId", eventId);
            model.addAttribute("inventory", inventoryService.getInventory(eventId));
            return "checkout/seats";
        }
    }

    @PostMapping("/release")
    public String release(
            @PathVariable Long eventId,
            @RequestParam String holdToken,
            RedirectAttributes redirectAttributes) {

        inventoryService.releaseHold(eventId, holdToken);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Los asientos retenidos fueron liberados.");
        return "redirect:/my/checkout/events/" + eventId + "/seats";
    }
}
