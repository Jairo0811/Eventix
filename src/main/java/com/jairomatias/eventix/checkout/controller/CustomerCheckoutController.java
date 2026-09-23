package com.jairomatias.eventix.checkout.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.checkout.dto.CustomerCheckoutForm;
import com.jairomatias.eventix.checkout.service.CustomerCheckoutService;
import com.jairomatias.eventix.eligibility.dto.SchoolEligibilityResult;
import com.jairomatias.eventix.eligibility.service.SchoolAlumniBenefitService;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

import jakarta.validation.Valid;

@Controller
public class CustomerCheckoutController {

    private static final PaymentProvider[] CUSTOMER_PAYMENT_PROVIDERS = {
            PaymentProvider.CARDNET,
            PaymentProvider.AZUL,
            PaymentProvider.QIK,
            PaymentProvider.STRIPE,
            PaymentProvider.PAYPAL,
            PaymentProvider.BANK_TRANSFER
    };

    private final CustomerCheckoutService checkoutService;
    private final SchoolAlumniBenefitService schoolAlumniBenefitService;

    public CustomerCheckoutController(
            CustomerCheckoutService checkoutService,
            SchoolAlumniBenefitService schoolAlumniBenefitService) {
        this.checkoutService = checkoutService;
        this.schoolAlumniBenefitService = schoolAlumniBenefitService;
    }

    @GetMapping("/my/checkout/events/{eventId}")
    public String checkout(
            @PathVariable Long eventId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", checkoutService.getForm(authentication.getName()));
        }
        prepareModel(eventId, authentication, model);
        return "checkout/form";
    }

    @PostMapping("/my/checkout/events/{eventId}")
    public String purchase(
            @PathVariable Long eventId,
            @Valid @ModelAttribute("checkoutForm") CustomerCheckoutForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareModel(eventId, authentication, model);
            return "checkout/form";
        }
        try {
            checkoutService.purchase(eventId, form, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Compra aprobada. Tus boletas ya están disponibles.");
            return "redirect:/my/tickets";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("checkout.purchase", exception.getMessage());
            prepareModel(eventId, authentication, model);
            return "checkout/form";
        }
    }

    @PostMapping("/my/checkout/events/{eventId}/school-alumni-benefit/verify")
    public String verifySchoolAlumniBenefit(
            @PathVariable Long eventId,
            @RequestParam String nationalId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            SchoolEligibilityResult result = schoolAlumniBenefitService.verifyForCheckout(
                    eventId,
                    authentication.getName(),
                    nationalId);
            addVerificationMessage(result, redirectAttributes);
        } catch (BusinessRuleException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/my/checkout/events/" + eventId;
    }

    private void prepareModel(Long eventId, Authentication authentication, Model model) {
        model.addAttribute("checkout", checkoutService.getCheckout(eventId, authentication.getName()));
        model.addAttribute("paymentProviders", CUSTOMER_PAYMENT_PROVIDERS);
        model.addAttribute(
                "schoolAlumniBenefit",
                schoolAlumniBenefitService.getCheckoutView(
                        eventId,
                        authentication.getName())
                        .orElse(null));
    }

    private void addVerificationMessage(
            SchoolEligibilityResult result,
            RedirectAttributes redirectAttributes) {
        switch (result.status()) {
            case "VERIFIED" -> redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Egresado verificado: " + result.memberName()
                            + ". El descuento se aplicará automáticamente a tu compra.");
            case "MANUAL_REVIEW" -> redirectAttributes.addFlashAttribute(
                    "warningMessage",
                    "El padrón contiene más de una coincidencia para el nombre oficial. "
                            + "La solicitud requiere revisión manual y el descuento aún no se aplicará.");
            case "NOT_FOUND" -> redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "El nombre oficial asociado a la cédula no aparece en el padrón de esta promoción.");
            case "IDENTITY_NOT_FOUND" -> redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "La fuente de identidad autorizada no encontró una identidad para la cédula indicada.");
            case "IDENTITY_PROVIDER_UNAVAILABLE" -> redirectAttributes.addFlashAttribute(
                    "warningMessage",
                    "La fuente de identidad no está disponible en este momento. "
                            + "No se aplicará el descuento hasta completar la verificación.");
            default -> redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "No fue posible validar el beneficio para egresados.");
        }
    }
}
