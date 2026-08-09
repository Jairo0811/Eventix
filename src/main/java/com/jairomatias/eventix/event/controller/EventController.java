package com.jairomatias.eventix.event.controller;

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

import com.jairomatias.eventix.category.service.EventCategoryService;
import com.jairomatias.eventix.event.dto.EventDetailsView;
import com.jairomatias.eventix.event.dto.EventForm;
import com.jairomatias.eventix.event.dto.EventListItem;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.service.EventManagementFacade;
import com.jairomatias.eventix.event.service.EventService;
import com.jairomatias.eventix.reservation.service.ReservationService;
import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/events")
public class EventController {

    private static final int PAGE_SIZE = 9;

    private final EventService eventService;
    private final EventManagementFacade eventManagementFacade;
    private final EventCategoryService categoryService;
    private final ReservationService reservationService;

    public EventController(
            EventService eventService,
            EventManagementFacade eventManagementFacade,
            EventCategoryService categoryService,
            ReservationService reservationService) {
        this.eventService = eventService;
        this.eventManagementFacade = eventManagementFacade;
        this.categoryService = categoryService;
        this.reservationService = reservationService;
    }

    @ModelAttribute("statuses")
    public EventStatus[] statuses() {
        return EventStatus.values();
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("startAt").ascending());

        Page<EventListItem> events = eventService.findAll(
                term,
                status,
                categoryId,
                organizerId,
                authentication.getName(),
                pageable);

        model.addAttribute("events", events);
        model.addAttribute("term", term);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("categories",
                categoryService.findActiveOptions());
        return "events/list";
    }

    @GetMapping("/new")
    public String createForm(
            Authentication authentication,
            Model model) {

        if (!model.containsAttribute("eventForm")) {
            model.addAttribute(
                    "eventForm",
                    eventService.getCreateForm(
                            authentication.getName()));
        }
        prepareFormModel(model, authentication, "create", null);
        return "events/form";
    }

    @PostMapping
    public String create(
            @Valid
            @ModelAttribute("eventForm")
            EventForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(
                    model,
                    authentication,
                    "create",
                    null);
            return "events/form";
        }

        try {
            Long eventId = eventManagementFacade.create(
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Evento creado correctamente.");
            return "redirect:/events/" + eventId;
        } catch (BusinessRuleException exception) {
            bindingResult.reject(
                    "event.create",
                    exception.getMessage());
            prepareFormModel(
                    model,
                    authentication,
                    "create",
                    null);
            return "events/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        EventDetailsView event = eventService.findById(
                id,
                authentication.getName());
        model.addAttribute("event", event);

        boolean administratorOrOperator =
                authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(
                        "ROLE_ADMINISTRATOR")
                        || authority.getAuthority().equals("ROLE_OPERATOR"));
        boolean organizerOwnsEvent = authentication.getPrincipal()
                instanceof UserPrincipal principal
                && "ORGANIZER".equals(principal.getRoleName())
                && event.organizerId().equals(principal.getId());
        boolean canViewReservations = administratorOrOperator
                || organizerOwnsEvent;
        boolean canManageReservations = administratorOrOperator;
        model.addAttribute("canViewReservations", canViewReservations);
        model.addAttribute("canManageReservations", canManageReservations);
        if (canViewReservations) {
            model.addAttribute(
                    "reservationMetrics",
                    reservationService.getEventMetrics(
                            id,
                            authentication.getName()));
        }
        return "events/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        if (!model.containsAttribute("eventForm")) {
            model.addAttribute(
                    "eventForm",
                    eventService.getUpdateForm(
                            id,
                            authentication.getName()));
        }
        prepareFormModel(model, authentication, "edit", id);
        return "events/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("eventForm")
            EventForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(
                    model,
                    authentication,
                    "edit",
                    id);
            return "events/form";
        }

        try {
            eventManagementFacade.update(
                    id,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Evento actualizado correctamente.");
            return "redirect:/events/" + id;
        } catch (BusinessRuleException exception) {
            bindingResult.reject(
                    "event.update",
                    exception.getMessage());
            prepareFormModel(
                    model,
                    authentication,
                    "edit",
                    id);
            return "events/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            eventService.delete(
                    id,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Evento eliminado correctamente.");
            return "redirect:/events";
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
            return "redirect:/events/" + id;
        }
    }

    private void prepareFormModel(
            Model model,
            Authentication authentication,
            String mode,
            Long eventId) {

        model.addAttribute("formMode", mode);
        model.addAttribute("eventId", eventId);
        model.addAttribute(
                "categories",
                categoryService.findActiveOptions());
        model.addAttribute(
                "organizers",
                eventService.findEligibleOrganizers(
                        authentication.getName()));
        model.addAttribute(
                "administrator",
                authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_ADMINISTRATOR")));
    }
}
