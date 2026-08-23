package com.jairomatias.eventix.sale.controller;

import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.sale.dto.PartialRefundForm;
import com.jairomatias.eventix.sale.service.PartialRefundService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Controller
@RequestMapping("/sales")
public class PartialRefundController {

    private final PartialRefundService refundService;

    public PartialRefundController(PartialRefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/{id}/partial-refund")
    public String refundTickets(
            @PathVariable Long id,
            @ModelAttribute PartialRefundForm form,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            BigDecimal amount = refundService.refundTickets(
                    id,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Reembolso procesado por RD$ " + amount.toPlainString()
                            + ". Las boletas seleccionadas fueron invalidadas.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/sales/" + id;
    }
}
