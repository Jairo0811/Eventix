package com.jairomatias.eventix.venue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.venue.dto.VenueRowForm;
import com.jairomatias.eventix.venue.dto.VenueSeatForm;
import com.jairomatias.eventix.venue.dto.VenueSectionForm;
import com.jairomatias.eventix.venue.entity.VenueSectionType;
import com.jairomatias.eventix.venue.service.VenueLayoutService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/venues/{venueId}/layout")
public class VenueLayoutController {

    private final VenueLayoutService layoutService;

    public VenueLayoutController(VenueLayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @GetMapping
    public String detail(@PathVariable Long venueId, Model model) {
        model.addAttribute("layout", layoutService.getLayout(venueId));
        return "venues/layout";
    }

    @GetMapping("/sections/new")
    public String sectionForm(@PathVariable Long venueId, Model model) {
        if (!model.containsAttribute("sectionForm")) {
            model.addAttribute("sectionForm", new VenueSectionForm());
        }
        model.addAttribute("venueId", venueId);
        model.addAttribute("sectionTypes", VenueSectionType.values());
        return "venues/section-form";
    }

    @PostMapping("/sections")
    public String addSection(
            @PathVariable Long venueId,
            @Valid @ModelAttribute("sectionForm") VenueSectionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("venueId", venueId);
            model.addAttribute("sectionTypes", VenueSectionType.values());
            return "venues/section-form";
        }

        try {
            layoutService.addSection(venueId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Sección creada correctamente.");
            return "redirect:/venues/" + venueId + "/layout";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            model.addAttribute("venueId", venueId);
            model.addAttribute("sectionTypes", VenueSectionType.values());
            return "venues/section-form";
        }
    }

    @GetMapping("/sections/{sectionId}/rows/new")
    public String rowForm(
            @PathVariable Long venueId,
            @PathVariable Long sectionId,
            Model model) {
        if (!model.containsAttribute("rowForm")) {
            model.addAttribute("rowForm", new VenueRowForm());
        }
        model.addAttribute("venueId", venueId);
        model.addAttribute("sectionId", sectionId);
        return "venues/row-form";
    }

    @PostMapping("/sections/{sectionId}/rows")
    public String addRow(
            @PathVariable Long venueId,
            @PathVariable Long sectionId,
            @Valid @ModelAttribute("rowForm") VenueRowForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("venueId", venueId);
            model.addAttribute("sectionId", sectionId);
            return "venues/row-form";
        }

        try {
            layoutService.addRow(venueId, sectionId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Fila creada correctamente.");
            return "redirect:/venues/" + venueId + "/layout";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            model.addAttribute("venueId", venueId);
            model.addAttribute("sectionId", sectionId);
            return "venues/row-form";
        }
    }

    @GetMapping("/sections/{sectionId}/rows/{rowId}/seats/new")
    public String seatForm(
            @PathVariable Long venueId,
            @PathVariable Long sectionId,
            @PathVariable Long rowId,
            Model model) {
        if (!model.containsAttribute("seatForm")) {
            model.addAttribute("seatForm", new VenueSeatForm());
        }
        model.addAttribute("venueId", venueId);
        model.addAttribute("sectionId", sectionId);
        model.addAttribute("rowId", rowId);
        return "venues/seat-form";
    }

    @PostMapping("/sections/{sectionId}/rows/{rowId}/seats")
    public String addSeat(
            @PathVariable Long venueId,
            @PathVariable Long sectionId,
            @PathVariable Long rowId,
            @Valid @ModelAttribute("seatForm") VenueSeatForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("venueId", venueId);
            model.addAttribute("sectionId", sectionId);
            model.addAttribute("rowId", rowId);
            return "venues/seat-form";
        }

        try {
            layoutService.addSeat(venueId, sectionId, rowId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Asiento creado correctamente.");
            return "redirect:/venues/" + venueId + "/layout";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            model.addAttribute("venueId", venueId);
            model.addAttribute("sectionId", sectionId);
            model.addAttribute("rowId", rowId);
            return "venues/seat-form";
        }
    }
}
