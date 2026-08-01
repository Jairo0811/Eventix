package com.jairomatias.eventix.reservation.controller;

import org.springframework.data.domain.Page;
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

import com.jairomatias.eventix.reservation.dto.CancellationForm;
import com.jairomatias.eventix.reservation.dto.ReservationForm;
import com.jairomatias.eventix.reservation.dto.ReservationListItem;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.service.ReservationService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private static final int PAGE_SIZE = 12;

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @ModelAttribute("statuses")
    public ReservationStatus[] statuses() {
        return ReservationStatus.values();
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReservationListItem> reservations =
                reservationService.findAll(
                        term,
                        status,
                        eventId,
                        authentication.getName(),
                        pageable);

        model.addAttribute("reservations", reservations);
        model.addAttribute("term", term);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute(
                "events",
                reservationService.findVisibleEvents(
                        authentication.getName()));
        model.addAttribute("canManage", canManage(authentication));

        if (eventId != null) {
            model.addAttribute(
                    "metrics",
                    reservationService.getEventMetrics(
                            eventId,
                            authentication.getName()));
        }
        return "reservations/list";
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(required = false) Long eventId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("reservationForm")) {
            model.addAttribute(
                    "reservationForm",
                    reservationService.getCreateForm(
                            eventId,
                            authentication.getName()));
        }
        prepareFormModel(model, "create", null);
        return "reservations/form";
    }

    @PostMapping
    public String create(
            @Valid
            @ModelAttribute("reservationForm")
            ReservationForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, "create", null);
            return "reservations/form";
        }

        try {
            Long reservationId = reservationService.create(
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Reservación creada. Los cupos permanecerán retenidos hasta su vencimiento.");
            return "redirect:/reservations/" + reservationId;
        } catch (BusinessRuleException exception) {
            bindingResult.reject(
                    "reservation.create",
                    exception.getMessage());
            prepareFormModel(model, "create", null);
            return "reservations/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "reservation",
                reservationService.findById(
                        id,
                        authentication.getName()));
        model.addAttribute("cancellationForm", new CancellationForm());
        model.addAttribute("canManage", canManage(authentication));
        return "reservations/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("reservationForm")) {
            model.addAttribute(
                    "reservationForm",
                    reservationService.getUpdateForm(
                            id,
                            authentication.getName()));
        }
        prepareFormModel(model, "edit", id);
        return "reservations/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("reservationForm")
            ReservationForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, "edit", id);
            return "reservations/form";
        }

        try {
            reservationService.update(
                    id,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Reservación actualizada correctamente.");
            return "redirect:/reservations/" + id;
        } catch (BusinessRuleException exception) {
            bindingResult.reject(
                    "reservation.update",
                    exception.getMessage());
            prepareFormModel(model, "edit", id);
            return "reservations/form";
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirm(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            reservationService.confirm(id, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Reservación confirmada correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/reservations/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("cancellationForm")
            CancellationForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().getFirst()
                            .getDefaultMessage());
            return "redirect:/reservations/" + id;
        }

        try {
            reservationService.cancel(
                    id,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Reservación cancelada y cupos liberados.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/reservations/" + id;
    }

    private void prepareFormModel(
            Model model,
            String mode,
            Long reservationId) {
        model.addAttribute("formMode", mode);
        model.addAttribute("reservationId", reservationId);
        model.addAttribute(
                "events",
                reservationService.findReservableEvents());
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(
                        "ROLE_ADMINISTRATOR")
                        || authority.getAuthority().equals(
                                "ROLE_OPERATOR"));
    }
}
