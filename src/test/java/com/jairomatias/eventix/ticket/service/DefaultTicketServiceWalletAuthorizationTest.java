package com.jairomatias.eventix.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.wallet.AppleWalletPassService;
import com.jairomatias.eventix.ticket.wallet.GoogleWalletPassService;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

class DefaultTicketServiceWalletAuthorizationTest {

    private DigitalTicketRepository ticketRepository;
    private UserRepository userRepository;
    private TicketDocumentService documentService;
    private GoogleWalletPassService googleWalletService;
    private AppleWalletPassService appleWalletService;
    private DefaultTicketService service;
    private DigitalTicket ticket;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(DigitalTicketRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        userRepository = mock(UserRepository.class);
        documentService = mock(TicketDocumentService.class);
        googleWalletService = mock(GoogleWalletPassService.class);
        appleWalletService = mock(AppleWalletPassService.class);
        service = new DefaultTicketService(
                ticketRepository,
                eventRepository,
                userRepository,
                documentService,
                googleWalletService,
                appleWalletService);
        ticket = mock(DigitalTicket.class);
        when(ticketRepository.findDetailedById(41L))
                .thenReturn(Optional.of(ticket));
        when(ticket.getAttendeeEmail()).thenReturn("owner@example.com");
        when(ticket.getEvent()).thenReturn(mock(Event.class));
    }

    @Test
    void ownerCanGenerateBothWalletArtifacts() {
        mockUser("owner@example.com", RoleName.USER);
        when(googleWalletService.createSaveUrl(ticket))
                .thenReturn("https://pay.google.com/gp/v/save/token");
        when(appleWalletService.createPass(ticket))
                .thenReturn(new byte[] {1, 2, 3});

        assertThat(service.createGoogleWalletUrl(41L, "owner@example.com"))
                .endsWith("/token");
        assertThat(service.createAppleWalletPass(41L, "owner@example.com"))
                .containsExactly(1, 2, 3);
    }

    @Test
    void anotherBuyerCannotGenerateWalletArtifacts() {
        mockUser("other@example.com", RoleName.USER);

        assertThatThrownBy(() -> service.createGoogleWalletUrl(
                41L, "other@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permiso");
        assertThatThrownBy(() -> service.createAppleWalletPass(
                41L, "other@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permiso");

        verify(googleWalletService, never()).createSaveUrl(ticket);
        verify(appleWalletService, never()).createPass(ticket);
    }

    @Test
    void pdfAndQrRemainAvailableWhenWalletProvidersAreDisabled() {
        mockUser("owner@example.com", RoleName.USER);
        when(googleWalletService.isAvailable()).thenReturn(false);
        when(appleWalletService.isAvailable()).thenReturn(false);
        when(documentService.createPdf(ticket)).thenReturn(new byte[] {7, 8});
        when(documentService.createQrPng(ticket)).thenReturn(new byte[] {9, 10});

        assertThat(service.createPdf(41L, "owner@example.com"))
                .containsExactly(7, 8);
        assertThat(service.createQrPng(41L, "owner@example.com"))
                .containsExactly(9, 10);
    }

    private void mockUser(String email, RoleName roleName) {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(roleName);
        when(user.getRole()).thenReturn(role);
        when(user.getEmail()).thenReturn(email);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                email, email)).thenReturn(Optional.of(user));
    }
}
