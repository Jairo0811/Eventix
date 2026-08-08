package com.jairomatias.eventix.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.dto.ScanForm;
import com.jairomatias.eventix.ticket.dto.ScanResultView;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.ScanOutcome;
import com.jairomatias.eventix.ticket.entity.TicketScanAttempt;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.repository.TicketScanAttemptRepository;
import com.jairomatias.eventix.ticket.security.ParsedTicketToken;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DefaultAccessControlServiceTest {

    private static final String LOGIN = "access@eventix.local";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 8, 18, 30);

    @Mock private DigitalTicketRepository ticketRepository;
    @Mock private TicketScanAttemptRepository attemptRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketCryptographyService cryptographyService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DigitalTicket ticket;
    @Mock private Event event;
    @Mock private User accessUser;

    private TicketingProperties properties;
    private DefaultAccessControlService service;

    @BeforeEach
    void setUp() {
        properties = new TicketingProperties();
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-08T22:30:00Z"),
                ZoneId.of("America/Santo_Domingo"));
        service = new DefaultAccessControlService(
                ticketRepository,
                attemptRepository,
                eventRepository,
                userRepository,
                cryptographyService,
                properties,
                eventPublisher,
                clock);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                LOGIN,
                LOGIN)).thenReturn(Optional.of(accessUser));
        when(cryptographyService.sha256(any())).thenReturn("a".repeat(64));
    }

    @Test
    void validSignedTicketRegistersFirstAccess() {
        ParsedTicketToken token = prepareKnownTicket(TicketStatus.ACTIVE);

        ScanResultView result = service.scan(
                form(false),
                LOGIN,
                "127.0.0.1");

        assertThat(result.outcome()).isEqualTo(ScanOutcome.VALID);
        assertThat(result.accepted()).isTrue();
        verify(ticket).markUsed(NOW);
        ArgumentCaptor<TicketScanAttempt> captor =
                ArgumentCaptor.forClass(TicketScanAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().isFirstAccess()).isTrue();
        assertThat(captor.getValue().isDuplicateAttempt()).isFalse();
        verify(cryptographyService).verify(ticket, token);
    }

    @Test
    void usedTicketIsRejectedAsDuplicateByDefault() {
        prepareKnownTicket(TicketStatus.USED);

        ScanResultView result = service.scan(
                form(false),
                LOGIN,
                "127.0.0.1");

        assertThat(result.outcome()).isEqualTo(ScanOutcome.DUPLICATE);
        assertThat(result.accepted()).isFalse();
        ArgumentCaptor<TicketScanAttempt> captor =
                ArgumentCaptor.forClass(TicketScanAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().isDuplicateAttempt()).isTrue();
    }

    @Test
    void malformedQrIsStoredOnlyAsAHash() {
        when(cryptographyService.parseQrPayload("invalid"))
                .thenReturn(Optional.empty());
        ScanForm form = form(false);
        form.setToken("invalid");

        ScanResultView result = service.scan(
                form,
                LOGIN,
                "127.0.0.1");

        assertThat(result.outcome()).isEqualTo(ScanOutcome.COUNTERFEIT);
        ArgumentCaptor<TicketScanAttempt> captor =
                ArgumentCaptor.forClass(TicketScanAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getTicket()).isNull();
    }

    private ParsedTicketToken prepareKnownTicket(TicketStatus status) {
        ParsedTicketToken parsed = new ParsedTicketToken(
                "TKT-ABCDEFGH23456789JKLM",
                "AF-ABCDEFGH23456789JKLM",
                "signature");
        when(cryptographyService.parseQrPayload(any()))
                .thenReturn(Optional.of(parsed));
        when(ticketRepository.findByUniqueCodeForUpdate(parsed.uniqueCode()))
                .thenReturn(Optional.of(ticket));
        when(cryptographyService.verify(ticket, parsed)).thenReturn(true);
        when(ticket.getStatus()).thenReturn(status);
        when(ticket.getEvent()).thenReturn(event);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getEndAt()).thenReturn(NOW.plusHours(2));
        when(event.getTitle()).thenReturn("Concierto Eventix");
        if (status == TicketStatus.ACTIVE) {
            when(ticket.getId()).thenReturn(41L);
        }
        when(ticket.getUniqueCode()).thenReturn(parsed.uniqueCode());
        when(ticket.getAttendeeName()).thenReturn("María Pérez");
        when(ticket.getTicketTypeName()).thenReturn("VIP");
        return parsed;
    }

    private ScanForm form(boolean reentry) {
        ScanForm form = new ScanForm();
        form.setToken(
                "EVX1.TKT-ABCDEFGH23456789JKLM."
                + "AF-ABCDEFGH23456789JKLM.signature");
        form.setDeviceIdentifier("Scanner puerta 1");
        form.setReentry(reentry);
        return form;
    }
}
