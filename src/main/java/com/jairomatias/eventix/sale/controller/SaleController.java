package com.jairomatias.eventix.sale.controller;

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

import com.jairomatias.eventix.payment.dto.PaymentForm;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.gateway.SimulationOutcome;
import com.jairomatias.eventix.sale.dto.SaleActionForm;
import com.jairomatias.eventix.sale.dto.SaleDetailsView;
import com.jairomatias.eventix.sale.dto.SaleForm;
import com.jairomatias.eventix.sale.dto.SaleListItem;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.service.SaleService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/sales")
public class SaleController {

    private static final int PAGE_SIZE = 12;

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @ModelAttribute("saleStatuses")
    public SaleStatus[] saleStatuses() {
        return SaleStatus.values();
    }

    @ModelAttribute("paymentProviders")
    public PaymentProvider[] paymentProviders() {
        return PaymentProvider.values();
    }

    @ModelAttribute("simulationOutcomes")
    public SimulationOutcome[] simulationOutcomes() {
        return SimulationOutcome.values();
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("createdAt").descending());
        Page<SaleListItem> sales = saleService.findAll(
                term,
                status,
                eventId,
                authentication.getName(),
                pageable);

        model.addAttribute("sales", sales);
        model.addAttribute(
                "summary",
                saleService.getSummary(authentication.getName()));
        model.addAttribute(
                "events",
                saleService.findVisibleEvents(authentication.getName()));
        model.addAttribute("term", term);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedEventId", eventId);
        return "sales/list";
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(required = false) Long reservationId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("saleForm")) {
            model.addAttribute(
                    "saleForm",
                    saleService.getCreateForm(
                            reservationId,
                            authentication.getName()));
        }
        prepareCreateModel(reservationId, authentication, model);
        return "sales/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("saleForm") SaleForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareCreateModel(form.getReservationId(), authentication, model);
            return "sales/form";
        }
        try {
            Long saleId = saleService.create(form, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Venta creada correctamente.");
            return "redirect:/sales/" + saleId;
        } catch (BusinessRuleException exception) {
            bindingResult.reject("sale.create", exception.getMessage());
            prepareCreateModel(form.getReservationId(), authentication, model);
            return "sales/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "sale",
                saleService.findById(id, authentication.getName()));
        model.addAttribute("paymentForm", new PaymentForm());
        model.addAttribute("saleActionForm", new SaleActionForm());
        return "sales/detail";
    }

    @GetMapping("/{id}/receipt")
    public String receipt(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        SaleDetailsView sale = saleService.findById(
                id,
                authentication.getName());
        if (sale.status() == SaleStatus.PENDING) {
            throw new BusinessRuleException(
                    "El comprobante está disponible después de procesar la venta.");
        }
        model.addAttribute("sale", sale);
        return "sales/receipt";
    }

    @PostMapping("/{id}/payments")
    public String processPayment(
            @PathVariable Long id,
            @Valid @ModelAttribute("paymentForm") PaymentForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().getFirst()
                            .getDefaultMessage());
            return "redirect:/sales/" + id;
        }
        try {
            boolean approved = saleService.processPayment(
                    id,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    approved ? "successMessage" : "errorMessage",
                    approved
                            ? "Pago simulado aprobado; la venta quedó pagada."
                            : "Pago simulado rechazado; puedes intentar nuevamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/sales/" + id;
    }

    @PostMapping("/{id}/refund")
    public String refund(
            @PathVariable Long id,
            @ModelAttribute("saleActionForm") SaleActionForm form,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            saleService.refund(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Venta reembolsada y cupos liberados.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/sales/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @ModelAttribute("saleActionForm") SaleActionForm form,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            saleService.cancel(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Venta cancelada y cupos liberados.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/sales/" + id;
    }

    private void prepareCreateModel(
            Long reservationId,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "reservations",
                saleService.findSaleableReservations());
        model.addAttribute(
                "ticketTypes",
                saleService.findTicketTypeOptions(
                        reservationId,
                        authentication.getName()));
        model.addAttribute("selectedReservationId", reservationId);
    }
}
