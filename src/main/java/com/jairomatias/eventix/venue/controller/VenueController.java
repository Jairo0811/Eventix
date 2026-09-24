package com.jairomatias.eventix.venue.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.venue.dto.VenueForm;
import com.jairomatias.eventix.venue.dto.VenueListItem;
import com.jairomatias.eventix.venue.service.VenueService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/venues")
public class VenueController {

    private static final int PAGE_SIZE = 10;

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("name").ascending());

        Page<VenueListItem> venues = venueService.findAll(term, active, pageable);
        model.addAttribute("venues", venues);
        model.addAttribute("term", term);
        model.addAttribute("selectedActive", active);
        return "venues/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("venueForm")) {
            model.addAttribute("venueForm", new VenueForm());
        }
        model.addAttribute("formMode", "create");
        return "venues/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("venueForm") VenueForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "venues/form";
        }

        try {
            venueService.create(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Recinto creado correctamente.");
            return "redirect:/venues";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            model.addAttribute("formMode", "create");
            return "venues/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("venueForm")) {
            model.addAttribute("venueForm", venueService.getForm(id));
        }
        model.addAttribute("venueId", id);
        model.addAttribute("formMode", "edit");
        return "venues/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("venueForm") VenueForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("venueId", id);
            model.addAttribute("formMode", "edit");
            return "venues/form";
        }

        try {
            venueService.update(id, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Recinto actualizado correctamente.");
            return "redirect:/venues";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            model.addAttribute("venueId", id);
            model.addAttribute("formMode", "edit");
            return "venues/form";
        }
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        venueService.activate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Recinto activado correctamente.");
        return "redirect:/venues";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        venueService.deactivate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Recinto desactivado correctamente.");
        return "redirect:/venues";
    }
}
