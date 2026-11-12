package com.jairomatias.eventix.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.dto.ScanForm;
import com.jairomatias.eventix.ticket.dto.ScanResultView;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.ScanOutcome;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.repository.TicketScanAttemptRepository;
import com.jairomatias.eventix.ticket.security.Ed25519TicketCryptographyService;
import com.jairomatias.eventix.ticket.security.SignedTicketPayload;
import com.jairomatias.eventix.ticket.security.TicketSigningPayload;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

class DefaultAccessControlRealCryptographyTest {

    private static final String LOGIN = "access@eventix.local";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 8, 18, 30);

    @Test
    void signedQrMovesFromValidToDuplicateAndCancelled() {
        TicketingProperties properties = new TicketingProperties();
        properties.setSigningKeyId("access-flow-test");
        Ed25519TicketCryptographyService cryptography =
                new Ed25519TicketCryptographyService(properties);
        TicketSigningPayload payload = new TicketSigningPayload(
                "TKT-ABCDEFGH23456789JKLM",
                "SAL-ABCDEFGH2345",
                8L,
                "asistente@example.com",
                "VIP",
                1,
                NOW.minusMinutes(30),
                "AF-ABCDEFGH23456789JKLM");
        SignedTicketPayload signed = cryptography.sign(payload);

        DigitalTicket ticket = ticket(payload, signed);
        Event event = ticket.getEvent();
        DigitalTicketRepository ticketRepository =
                mock(DigitalTicketRepository.class);
        TicketScanAttemptRepository attemptRepository =
                mock(TicketScanAttemptRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ApplicationEventPublisher eventPublisher =
                mock(ApplicationEventPublisher.class);
        User accessUser = mock(User.class);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                LOGIN, LOGIN)).thenReturn(Optional.of(accessUser));
        when(ticketRepository.findByUniqueCodeForUpdate(payload.uniqueCode()))
                .thenReturn(Optional.of(ticket));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getEndAt()).thenReturn(NOW.plusHours(2));
        when(event.getTitle()).thenReturn("Concierto Eventix");
        when(ticket.getAttendeeName()).thenReturn("María Pérez");
        when(ticket.getId()).thenReturn(41L);

        DefaultAccessControlService service = new DefaultAccessControlService(
                ticketRepository,
                attemptRepository,
                eventRepository,
                userRepository,
                cryptography,
                properties,
                eventPublisher,
                Clock.fixed(
                        Instant.parse("2026-08-08T22:30:00Z"),
                        ZoneId.of("America/Santo_Domingo")));
        ScanForm form = new ScanForm();
        form.setToken(cryptography.createQrPayload(ticket));
        form.setDeviceIdentifier("Scanner puerta 1");
        form.setReentry(false);

        when(ticket.getStatus()).thenReturn(TicketStatus.ACTIVE);
        ScanResultView first = service.scan(form, LOGIN, "127.0.0.1");
        assertThat(first.outcome()).isEqualTo(ScanOutcome.VALID);
        assertThat(first.accepted()).isTrue();

        when(ticket.getStatus()).thenReturn(TicketStatus.USED);
        ScanResultView duplicate = service.scan(form, LOGIN, "127.0.0.1");
        assertThat(duplicate.outcome()).isEqualTo(ScanOutcome.DUPLICATE);
        assertThat(duplicate.accepted()).isFalse();

        when(ticket.getStatus()).thenReturn(TicketStatus.CANCELLED);
        ScanResultView cancelled = service.scan(form, LOGIN, "127.0.0.1");
        assertThat(cancelled.outcome()).isEqualTo(ScanOutcome.CANCELLED);
        assertThat(cancelled.accepted()).isFalse();
    }

    private DigitalTicket ticket(
            TicketSigningPayload payload,
            SignedTicketPayload signed) {
        DigitalTicket ticket = mock(DigitalTicket.class);
        Sale sale = mock(Sale.class);
        Event event = mock(Event.class);
        when(ticket.getUniqueCode()).thenReturn(payload.uniqueCode());
        when(ticket.getSale()).thenReturn(sale);
        when(sale.getReferenceCode()).thenReturn(payload.saleReference());
        when(ticket.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(payload.eventId());
        when(ticket.getAttendeeEmail()).thenReturn(payload.attendeeEmail());
        when(ticket.getTicketTypeName()).thenReturn(payload.ticketTypeName());
        when(ticket.getSequenceNumber()).thenReturn(payload.sequenceNumber());
        when(ticket.getIssuedAt()).thenReturn(payload.issuedAt());
        when(ticket.getAntiFraudCode()).thenReturn(payload.antiFraudCode());
        when(ticket.getSignedPayloadHash()).thenReturn(signed.payloadHash());
        when(ticket.getDigitalSignature()).thenReturn(signed.signature());
        when(ticket.getSignatureKeyId()).thenReturn(signed.keyId());
        return ticket;
    }
}
