package com.jairomatias.eventix.venue.controller;

import java.util.List;
import java.util.Optional;

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
import com.jairomatias.eventix.venue.dto.EventSeatView;
import com.jairomatias.eventix.venue.dto.SeatHoldResult;
import com.jairomatias.eventix.venue.dto.SeatSelectionForm;
import com.jairomatias.eventix.venue.dto.TicketSeatingRule;
import com.jairomatias.eventix.venue.service.CustomerSeatingRuleService;
import com.jairomatias.eventix.venue.service.EventSeatInventoryService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/my/checkout/events/{eventId}/seats")
public class CustomerSeatSelectionController {

    private final EventSeatInventoryService inventoryService;
    private final CustomerSeatingRuleService seatingRuleService;

    public CustomerSeatSelectionController(
            EventSeatInventoryService inventoryService,
            CustomerSeatingRuleService seatingRuleService) {
        this.inventoryService = inventoryService;
        this.seatingRuleService = seatingRuleService;
    }

    @GetMapping
    public String select(
            @PathVariable Long eventId,
            @RequestParam(required = false) Long ticketTypeId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<SeatHoldResult> activeHold = inventoryService.getActiveHold(
                eventId,
                authentication.getName());

        if (ticketTypeId == null && activeHold.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "warningMessage",
                    "Selecciona primero el tipo de entrada para mostrar la sección de asientos correcta.");
            return "redirect:/my/checkout/events/" + eventId;
        }

        if (!model.containsAttribute("seatSelectionForm")) {
            model.addAttribute("seatSelectionForm", new SeatSelectionForm());
        }

        try {
            prepareModel(eventId, ticketTypeId, authentication, activeHold, model);
            return "checkout/seats";
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/my/checkout/events/" + eventId;
        }
    }

    @PostMapping
    public String hold(
            @PathVariable Long eventId,
            @RequestParam Long ticketTypeId,
            @Valid @ModelAttribute("seatSelectionForm") SeatSelectionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        TicketSeatingRule rule;
        try {
            rule = seatingRuleService.resolve(eventId, ticketTypeId);
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/my/checkout/events/" + eventId;
        }

        if (bindingResult.hasErrors()) {
            prepareModel(
                    eventId,
                    ticketTypeId,
                    authentication,
                    inventoryService.getActiveHold(eventId, authentication.getName()),
                    model);
            return "checkout/seats";
        }

        try {
            SeatHoldResult hold = inventoryService.holdSeats(
                    eventId,
                    form.getSeatIds(),
                    rule.sectionId(),
                    authentication.getName());
            addHoldFlashAttributes(hold, redirectAttributes, "Asientos retenidos durante 10 minutos.");
            return seatSelectionRedirect(eventId, ticketTypeId);
        } catch (BusinessRuleException exception) {
            bindingResult.reject("seat.hold", exception.getMessage());
            prepareModel(
                    eventId,
                    ticketTypeId,
                    authentication,
                    inventoryService.getActiveHold(eventId, authentication.getName()),
                    model);
            return "checkout/seats";
        }
    }

    @PostMapping("/best-available")
    public String holdBestAvailable(
            @PathVariable Long eventId,
            @RequestParam Long ticketTypeId,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "false") boolean accessibilityRequired,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            TicketSeatingRule rule = seatingRuleService.resolve(eventId, ticketTypeId);
            SeatHoldResult hold = inventoryService.holdBestAvailableSeats(
                    eventId,
                    quantity,
                    accessibilityRequired,
                    rule.sectionId(),
                    authentication.getName());
            addHoldFlashAttributes(
                    hold,
                    redirectAttributes,
                    "Eventix encontró y retuvo los mejores asientos contiguos disponibles en tu sección.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return seatSelectionRedirect(eventId, ticketTypeId);
    }

    @PostMapping("/release")
    public String release(
            @PathVariable Long eventId,
            @RequestParam String holdToken,
            @RequestParam(required = false) Long ticketTypeId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        inventoryService.releaseHold(
                eventId,
                holdToken,
                authentication.getName());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Los asientos retenidos fueron liberados.");
        return ticketTypeId == null
                ? "redirect:/my/checkout/events/" + eventId
                : seatSelectionRedirect(eventId, ticketTypeId);
    }

    private void prepareModel(
            Long eventId,
            Long ticketTypeId,
            Authentication authentication,
            Optional<SeatHoldResult> activeHold,
            Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("ticketTypeId", ticketTypeId);

        List<EventSeatView> inventory;
        if (ticketTypeId == null) {
            inventory = inventoryService.getInventory(eventId);
        } else {
            TicketSeatingRule rule = seatingRuleService.resolve(eventId, ticketTypeId);
            inventory = inventoryService.getInventory(eventId, rule.sectionId());
        }
        model.addAttribute("inventory", inventory);
        model.addAttribute(
                "hasPositionedSeats",
                inventory.stream().anyMatch(seat ->
                        seat.xPosition() != null && seat.yPosition() != null));
        model.addAttribute(
                "hasUnpositionedSeats",
                inventory.stream().anyMatch(seat ->
                        seat.xPosition() == null || seat.yPosition() == null));

        activeHold.ifPresent(hold -> addHoldModelAttributes(hold, model));
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

    private String seatSelectionRedirect(Long eventId, Long ticketTypeId) {
        return "redirect:/my/checkout/events/" + eventId
                + "/seats?ticketTypeId=" + ticketTypeId;
    }
}
