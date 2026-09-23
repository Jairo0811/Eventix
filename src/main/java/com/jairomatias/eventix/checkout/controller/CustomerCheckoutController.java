package com.jairomatias.eventix.checkout.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.checkout.dto.CustomerCheckoutForm;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutQuote;
import com.jairomatias.eventix.checkout.dto.CustomerCheckoutQuoteRequest;
import com.jairomatias.eventix.checkout.service.CustomerCheckoutService;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.gateway.azul.AzulWalletProperties;
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
    private final AzulWalletProperties walletProperties;

    public CustomerCheckoutController(
            CustomerCheckoutService checkoutService,
            AzulWalletProperties walletProperties) {
        this.checkoutService = checkoutService;
        this.walletProperties = walletProperties;
    }

    @GetMapping("/my/checkout/events/{eventId}")
    public String checkout(
            @PathVariable Long eventId,
            Authentication authentication,
            Model model) {
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute(
                    "checkoutForm",
                    checkoutService.getForm(authentication.getName()));
        }
        prepareModel(eventId, authentication, model);
        return "checkout/form";
    }

    @PostMapping(
            value = "/my/checkout/events/{eventId}/quote",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CustomerCheckoutQuote quote(
            @PathVariable Long eventId,
            @RequestBody CustomerCheckoutQuoteRequest request,
            Authentication authentication) {
        return checkoutService.quote(
                eventId,
                request,
                authentication.getName());
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
            clearWalletData(form);
            prepareModel(eventId, authentication, model);
            return "checkout/form";
        }
        try {
            checkoutService.purchase(
                    eventId,
                    form,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Compra aprobada. Tus boletas ya están disponibles.");
            return "redirect:/my/tickets";
        } catch (BusinessRuleException exception) {
            clearWalletData(form);
            bindingResult.reject(
                    "checkout.purchase",
                    exception.getMessage());
            prepareModel(eventId, authentication, model);
            return "checkout/form";
        }
    }

    private void clearWalletData(CustomerCheckoutForm form) {
        form.setWalletToken(null);
        if (form.getProvider() != null && form.getProvider().isDigitalWallet()) {
            form.setProvider(PaymentProvider.CARDNET);
        }
    }

    private void prepareModel(
            Long eventId,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "checkout",
                checkoutService.getCheckout(
                        eventId,
                        authentication.getName()));
        model.addAttribute("paymentProviders", CUSTOMER_PAYMENT_PROVIDERS);
        model.addAttribute(
                "googlePay",
                new GooglePayCheckoutConfig(
                        walletProperties.isGooglePayReady(),
                        walletProperties.getEnvironment().name(),
                        walletProperties.googleGatewayMerchantId(),
                        walletProperties.googlePayMerchantId(),
                        walletProperties.getMerchantDisplayName(),
                        "DO"));
    }

    public record GooglePayCheckoutConfig(
            boolean enabled,
            String environment,
            String gatewayMerchantId,
            String merchantId,
            String merchantName,
            String countryCode) {
    }
}
