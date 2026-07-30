package com.jairomatias.eventix.category.controller;

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

import com.jairomatias.eventix.category.dto.CategoryForm;
import com.jairomatias.eventix.category.dto.CategoryListItem;
import com.jairomatias.eventix.category.service.EventCategoryService;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/categories")
public class EventCategoryController {

    private static final int PAGE_SIZE = 10;

    private final EventCategoryService categoryService;

    public EventCategoryController(
            EventCategoryService categoryService) {
        this.categoryService = categoryService;
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

        Page<CategoryListItem> categories =
                categoryService.findAll(term, active, pageable);

        model.addAttribute("categories", categories);
        model.addAttribute("term", term);
        model.addAttribute("selectedActive", active);
        return "categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute(
                    "categoryForm",
                    new CategoryForm());
        }
        model.addAttribute("formMode", "create");
        return "categories/form";
    }

    @PostMapping
    public String create(
            @Valid
            @ModelAttribute("categoryForm")
            CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "categories/form";
        }

        try {
            categoryService.create(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Categoría creada correctamente.");
            return "redirect:/categories";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            model.addAttribute("formMode", "create");
            return "categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model) {

        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute(
                    "categoryForm",
                    categoryService.getForm(id));
        }
        model.addAttribute("categoryId", id);
        model.addAttribute("formMode", "edit");
        return "categories/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("categoryForm")
            CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            model.addAttribute("formMode", "edit");
            return "categories/form";
        }

        try {
            categoryService.update(id, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Categoría actualizada correctamente.");
            return "redirect:/categories";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            model.addAttribute("categoryId", id);
            model.addAttribute("formMode", "edit");
            return "categories/form";
        }
    }

    @PostMapping("/{id}/activate")
    public String activate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        categoryService.activate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Categoría activada correctamente.");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        categoryService.deactivate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Categoría desactivada correctamente.");
        return "redirect:/categories";
    }
}
