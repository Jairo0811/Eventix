package com.jairomatias.eventix.ticket.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.AppleWalletRegistration;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.AppleWalletRegistrationRepository;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;

@ExtendWith(MockitoExtension.class)
class AppleWalletWebServiceTest {

    @Mock private DigitalTicketRepository ticketRepository;
    @Mock private AppleWalletRegistrationRepository registrationRepository;
    @Mock private AppleWalletPassService passService;
    @Mock private DigitalTicket ticket;

    private AppleWalletWebService service;

    @BeforeEach
    void setUp() {
        TicketingProperties properties = new TicketingProperties();
        TicketingProperties.AppleWallet apple = properties.getAppleWallet();
        apple.setEnabled(true);
        apple.setPassTypeIdentifier("pass.com.example.eventix");
        apple.setTeamIdentifier("TEAM123");
        apple.setCertificateP12("base64:AA==");
        apple.setWwdrCertificate("AA==");
        apple.setWebServiceUrl(
                "https://eventix.example.com/api/wallet/apple/v1");
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-08T22:30:00Z"),
                ZoneId.of("America/Santo_Domingo"));
        service = new AppleWalletWebService(
                ticketRepository,
                registrationRepository,
                passService,
                properties,
                clock);
    }

    @Test
    void registersAuthorizedDevice() {
        prepareTicket();
        when(ticket.getId()).thenReturn(41L);
        when(registrationRepository
                .findByDeviceLibraryIdentifierAndTicket_Id(
                        "device-1",
                        41L))
                .thenReturn(Optional.empty());

        boolean created = service.register(
                "device-1",
                "pass.com.example.eventix",
                "TKT-ABC",
                "ApplePass secret-token",
                "push-token");

        assertThat(created).isTrue();
        verify(registrationRepository).save(
                any(AppleWalletRegistration.class));
    }

    @Test
    void rejectsInvalidPassAuthorization() {
        prepareTicket();

        assertThatThrownBy(() -> service.register(
                "device-1",
                "pass.com.example.eventix",
                "TKT-ABC",
                "ApplePass wrong",
                "push-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception)
                                .getStatusCode().value())
                        .isEqualTo(401));
    }

    private void prepareTicket() {
        when(ticketRepository.findByUniqueCode("TKT-ABC"))
                .thenReturn(Optional.of(ticket));
        when(ticket.getAntiFraudCode()).thenReturn("secret-token");
    }
}
