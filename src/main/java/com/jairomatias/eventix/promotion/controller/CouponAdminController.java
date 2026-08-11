package com.jairomatias.eventix.promotion.controller;

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

import com.jairomatias.eventix.promotion.dto.CouponForm;
import com.jairomatias.eventix.promotion.dto.CouponListItem;
import com.jairomatias.eventix.promotion.entity.DiscountType;
import com.jairomatias.eventix.promotion.service.CouponAdminService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/promotions/coupons")
public class CouponAdminController {

    private static final int PAGE_SIZE = 12;

    private final CouponAdminService couponService;

    public CouponAdminController(CouponAdminService couponService) {
        this.couponService = couponService;
    }

    @ModelAttribute("discountTypes")
    public DiscountType[] discountTypes() {
        return DiscountType.values();
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
                Sort.by("createdAt").descending());
        Page<CouponListItem> coupons = couponService.findAll(
                term,
                active,
                pageable);
        model.addAttribute("coupons", coupons);
        model.addAttribute("term", term);
        model.addAttribute("selectedActive", active);
        return "promotions/coupons/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("couponForm")) {
            model.addAttribute("couponForm", new CouponForm());
        }
        prepareFormModel("create", null, model);
        return "promotions/coupons/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("couponForm") CouponForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel("create", null, model);
            return "promotions/coupons/form";
        }
        try {
            couponService.create(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cupón creado correctamente.");
            return "redirect:/promotions/coupons";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
        } catch (BusinessRuleException exception) {
            bindingResult.reject("coupon.create", exception.getMessage());
        }
        prepareFormModel("create", null, model);
        return "promotions/coupons/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("couponForm")) {
            model.addAttribute("couponForm", couponService.getForm(id));
        }
        prepareFormModel("edit", id, model);
        return "promotions/coupons/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("couponForm") CouponForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel("edit", id, model);
            return "promotions/coupons/form";
        }
        try {
            couponService.update(id, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cupón actualizado correctamente.");
            return "redirect:/promotions/coupons";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
        } catch (BusinessRuleException exception) {
            bindingResult.reject("coupon.update", exception.getMessage());
        }
        prepareFormModel("edit", id, model);
        return "promotions/coupons/form";
    }

    @PostMapping("/{id}/activate")
    public String activate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        couponService.activate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Cupón activado correctamente.");
        return "redirect:/promotions/coupons";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        couponService.deactivate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Cupón desactivado correctamente.");
        return "redirect:/promotions/coupons";
    }

    private void prepareFormModel(
            String mode,
            Long couponId,
            Model model) {
        model.addAttribute("formMode", mode);
        model.addAttribute("couponId", couponId);
        model.addAttribute("events", couponService.findEventOptions());
    }
}
