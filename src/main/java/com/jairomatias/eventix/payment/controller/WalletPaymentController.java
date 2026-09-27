package com.jairomatias.eventix.payment.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.gateway.azul.AzulSoapClient;
import com.jairomatias.eventix.payment.gateway.azul.AzulWalletProperties;
import com.jairomatias.eventix.payment.service.WalletPaymentService;
import com.jairomatias.eventix.sale.dto.SaleDetailsView;
import com.jairomatias.eventix.sale.service.SaleService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Controller
public class WalletPaymentController {

    private final WalletPaymentService walletPaymentService;
    private final SaleService saleService;
    private final AzulWalletProperties properties;
    private final AzulSoapClient azulSoapClient;

    public WalletPaymentController(
            WalletPaymentService walletPaymentService,
            SaleService saleService,
            AzulWalletProperties properties,
            AzulSoapClient azulSoapClient) {
        this.walletPaymentService = walletPaymentService;
        this.saleService = saleService;
        this.properties = properties;
        this.azulSoapClient = azulSoapClient;
    }

    @GetMapping("/api/payments/wallets/config/{saleId}")
    @ResponseBody
    public WalletClientConfig config(
            @PathVariable Long saleId,
            Authentication authentication) {
        SaleDetailsView sale = saleService.findById(
                saleId,
                authentication.getName());
        return new WalletClientConfig(
                properties.isReady(),
                properties.isGooglePayReady(),
                properties.getEnvironment().name(),
                properties.googleGatewayMerchantId(),
                properties.googlePayMerchantId(),
                properties.getMerchantDisplayName(),
                properties.appleMerchantIdentifier(),
                properties.getInitiativeContext() != null
                        && !properties.getInitiativeContext().isBlank(),
                sale.currency(),
                sale.total().toPlainString(),
                "DO");
    }

    @GetMapping(
            value = "/api/payments/wallets/apple/session",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public ResponseEntity<String> appleSession() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(azulSoapClient.createApplePaySession());
    }

    @PostMapping("/sales/{id}/wallet-payments")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR')")
    public String charge(
            @PathVariable Long id,
            @RequestParam PaymentProvider provider,
            @RequestParam String walletToken,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            boolean approved = walletPaymentService.charge(
                    id,
                    provider,
                    walletToken,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    approved ? "successMessage" : "errorMessage",
                    approved
                            ? provider.getDisplayName()
                                    + " aprobado; la venta quedó pagada."
                            : provider.getDisplayName()
                                    + " fue rechazado por la pasarela.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/sales/" + id;
    }

    public record WalletClientConfig(
            boolean enabled,
            boolean googlePayEnabled,
            String environment,
            String googleGatewayMerchantId,
            String googleMerchantId,
            String merchantDisplayName,
            String appleMerchantIdentifier,
            boolean applePayEnabled,
            String currency,
            String totalPrice,
            String countryCode) {
    }
}
