package com.jairomatias.eventix.ticket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jairomatias.eventix.ticket.dto.ApplePassUpdates;
import com.jairomatias.eventix.ticket.dto.AppleLogRequest;
import com.jairomatias.eventix.ticket.dto.ApplePushTokenRequest;
import com.jairomatias.eventix.ticket.wallet.AppleWalletWebService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/wallet/apple/v1")
public class AppleWalletWebController {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AppleWalletWebController.class);
    private static final MediaType PASSBOOK =
            MediaType.parseMediaType("application/vnd.apple.pkpass");

    private final AppleWalletWebService webService;

    public AppleWalletWebController(AppleWalletWebService webService) {
        this.webService = webService;
    }

    @PostMapping(
            "/devices/{deviceIdentifier}/registrations/"
            + "{passTypeIdentifier}/{serialNumber}")
    public ResponseEntity<Void> register(
            @PathVariable String deviceIdentifier,
            @PathVariable String passTypeIdentifier,
            @PathVariable String serialNumber,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ApplePushTokenRequest request) {
        boolean created = webService.register(
                deviceIdentifier,
                passTypeIdentifier,
                serialNumber,
                authorization,
                request.pushToken());
        return ResponseEntity.status(
                        created ? HttpStatus.CREATED : HttpStatus.OK)
                .build();
    }

    @DeleteMapping(
            "/devices/{deviceIdentifier}/registrations/"
            + "{passTypeIdentifier}/{serialNumber}")
    public ResponseEntity<Void> unregister(
            @PathVariable String deviceIdentifier,
            @PathVariable String passTypeIdentifier,
            @PathVariable String serialNumber,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        webService.unregister(
                deviceIdentifier,
                passTypeIdentifier,
                serialNumber,
                authorization);
        return ResponseEntity.ok().build();
    }

    @GetMapping(
            "/devices/{deviceIdentifier}/registrations/"
            + "{passTypeIdentifier}")
    public ApplePassUpdates updates(
            @PathVariable String deviceIdentifier,
            @PathVariable String passTypeIdentifier,
            @RequestParam(required = false) String passesUpdatedSince) {
        return webService.findUpdates(
                deviceIdentifier,
                passTypeIdentifier,
                passesUpdatedSince);
    }

    @GetMapping(
            value = "/passes/{passTypeIdentifier}/{serialNumber}",
            produces = "application/vnd.apple.pkpass")
    public ResponseEntity<byte[]> latestPass(
            @PathVariable String passTypeIdentifier,
            @PathVariable String serialNumber,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok()
                .contentType(PASSBOOK)
                .body(webService.latestPass(
                        passTypeIdentifier,
                        serialNumber,
                        authorization));
    }

    @PostMapping("/log")
    public ResponseEntity<Void> log(
            @Valid @RequestBody(required = false) AppleLogRequest payload) {
        int entryCount = payload == null || payload.logs() == null
                ? 0
                : payload.logs().size();
        LOGGER.info(
                "Apple Wallet reportó un diagnóstico con {} campos.",
                entryCount);
        return ResponseEntity.ok().build();
    }
}
