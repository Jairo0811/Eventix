package com.jairomatias.eventix.payment.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jairomatias.eventix.payment.gateway.azul.AzulWalletProperties;

@Controller
public class ApplePayDomainAssociationController {

    private final AzulWalletProperties properties;

    public ApplePayDomainAssociationController(
            AzulWalletProperties properties) {
        this.properties = properties;
    }

    @GetMapping(
            value = "/.well-known/apple-developer-merchantid-domain-association",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> domainAssociation() {
        if (!StringUtils.hasText(properties.getAppleDomainAssociation())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(properties.getAppleDomainAssociation());
    }
}
