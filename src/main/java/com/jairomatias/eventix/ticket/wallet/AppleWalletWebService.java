package com.jairomatias.eventix.ticket.wallet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.dto.ApplePassUpdates;
import com.jairomatias.eventix.ticket.entity.AppleWalletRegistration;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.AppleWalletRegistrationRepository;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;

@Service
public class AppleWalletWebService {

    private static final ZoneId EVENTIX_ZONE =
            ZoneId.of("America/Santo_Domingo");

    private final DigitalTicketRepository ticketRepository;
    private final AppleWalletRegistrationRepository registrationRepository;
    private final AppleWalletPassService passService;
    private final TicketingProperties properties;
    private final Clock clock;

    @Autowired
    public AppleWalletWebService(
            DigitalTicketRepository ticketRepository,
            AppleWalletRegistrationRepository registrationRepository,
            AppleWalletPassService passService,
            TicketingProperties properties) {
        this(
                ticketRepository,
                registrationRepository,
                passService,
                properties,
                Clock.systemDefaultZone());
    }

    AppleWalletWebService(
            DigitalTicketRepository ticketRepository,
            AppleWalletRegistrationRepository registrationRepository,
            AppleWalletPassService passService,
            TicketingProperties properties,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.registrationRepository = registrationRepository;
        this.passService = passService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public boolean register(
            String deviceIdentifier,
            String passTypeIdentifier,
            String serialNumber,
            String authorization,
            String pushToken) {
        DigitalTicket ticket = authorizedTicket(
                passTypeIdentifier,
                serialNumber,
                authorization);
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        AppleWalletRegistration registration = registrationRepository
                .findByDeviceLibraryIdentifierAndTicket_Id(
                        deviceIdentifier,
                        ticket.getId())
                .orElse(null);
        if (registration == null) {
            registrationRepository.save(new AppleWalletRegistration(
                    ticket,
                    requireIdentifier(deviceIdentifier, 160),
                    requireIdentifier(pushToken, 200),
                    now));
            return true;
        }
        registration.updatePushToken(
                requireIdentifier(pushToken, 200),
                now);
        return false;
    }

    @Transactional
    public void unregister(
            String deviceIdentifier,
            String passTypeIdentifier,
            String serialNumber,
            String authorization) {
        DigitalTicket ticket = authorizedTicket(
                passTypeIdentifier,
                serialNumber,
                authorization);
        registrationRepository.deleteByDeviceLibraryIdentifierAndTicket_Id(
                deviceIdentifier,
                ticket.getId());
    }

    @Transactional(readOnly = true)
    public ApplePassUpdates findUpdates(
            String deviceIdentifier,
            String passTypeIdentifier,
            String updatedSince) {
        requireConfiguredPassType(passTypeIdentifier);
        LocalDateTime threshold = parseUpdateTag(updatedSince);
        List<DigitalTicket> tickets = registrationRepository
                .findAllByDeviceLibraryIdentifierAndTicket_PassUpdatedAtAfter(
                        requireIdentifier(deviceIdentifier, 160),
                        threshold)
                .stream()
                .map(AppleWalletRegistration::getTicket)
                .distinct()
                .sorted(Comparator.comparing(
                        DigitalTicket::getPassUpdatedAt))
                .toList();
        String lastUpdated = tickets.stream()
                .map(DigitalTicket::getPassUpdatedAt)
                .max(Comparator.naturalOrder())
                .map(this::toUpdateTag)
                .orElseGet(() -> toUpdateTag(threshold));
        return new ApplePassUpdates(
                tickets.stream()
                        .map(DigitalTicket::getUniqueCode)
                        .toList(),
                lastUpdated);
    }

    @Transactional(readOnly = true)
    public byte[] latestPass(
            String passTypeIdentifier,
            String serialNumber,
            String authorization) {
        return passService.createPass(authorizedTicket(
                passTypeIdentifier,
                serialNumber,
                authorization));
    }

    private DigitalTicket authorizedTicket(
            String passTypeIdentifier,
            String serialNumber,
            String authorization) {
        requireConfiguredPassType(passTypeIdentifier);
        DigitalTicket ticket = ticketRepository
                .findByUniqueCode(serialNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND));
        String expected = "ApplePass " + ticket.getAntiFraudCode();
        if (authorization == null
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        authorization.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ticket;
    }

    private void requireConfiguredPassType(String value) {
        if (!properties.getAppleWallet().configured()
                || !properties.getAppleWallet()
                        .getPassTypeIdentifier()
                        .equals(value)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private String requireIdentifier(String value, int maximumLength) {
        if (value == null || value.isBlank()
                || value.length() > maximumLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private LocalDateTime parseUpdateTag(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.ofInstant(Instant.EPOCH, EVENTIX_ZONE);
        }
        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(value)),
                    EVENTIX_ZONE);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "passesUpdatedSince no es válido.");
        }
    }

    private String toUpdateTag(LocalDateTime value) {
        ZoneOffset offset = EVENTIX_ZONE.getRules().getOffset(
                value.atZone(EVENTIX_ZONE).toInstant());
        return Long.toString(value.toEpochSecond(offset));
    }
}
