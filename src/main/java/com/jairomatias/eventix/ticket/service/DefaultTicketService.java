package com.jairomatias.eventix.ticket.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.ticket.dto.TicketDetailsView;
import com.jairomatias.eventix.ticket.dto.TicketListItem;
import com.jairomatias.eventix.ticket.dto.TicketSummary;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.wallet.AppleWalletPassService;
import com.jairomatias.eventix.ticket.wallet.GoogleWalletPassService;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultTicketService implements TicketService {

    private final DigitalTicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketDocumentService documentService;
    private final GoogleWalletPassService googleWalletService;
    private final AppleWalletPassService appleWalletService;

    public DefaultTicketService(
            DigitalTicketRepository ticketRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            TicketDocumentService documentService,
            GoogleWalletPassService googleWalletService,
            AppleWalletPassService appleWalletService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.documentService = documentService;
        this.googleWalletService = googleWalletService;
        this.appleWalletService = appleWalletService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public Page<TicketListItem> findAll(
            String term,
            TicketStatus status,
            Long eventId,
            String authenticatedLogin,
            Pageable pageable) {
        User actor = findActor(authenticatedLogin);
        Long organizerId = organizerFilter(actor);
        return ticketRepository.search(
                        normalize(term),
                        status,
                        eventId,
                        organizerId,
                        pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public TicketSummary getSummary(
            Long eventId,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        Long organizerId = organizerFilter(actor);
        return new TicketSummary(
                ticketRepository.countVisible(eventId, organizerId),
                ticketRepository.countVisibleByStatus(
                        TicketStatus.ACTIVE,
                        eventId,
                        organizerId),
                ticketRepository.countVisibleByStatus(
                        TicketStatus.USED,
                        eventId,
                        organizerId),
                ticketRepository.countVisibleByStatus(
                        TicketStatus.CANCELLED,
                        eventId,
                        organizerId),
                ticketRepository.countVisibleByStatus(
                        TicketStatus.EXPIRED,
                        eventId,
                        organizerId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public TicketDetailsView findById(
            Long id,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        DigitalTicket ticket = findTicket(id);
        ensureCanView(ticket, actor);
        return toDetailsView(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public List<TicketListItem> findBySale(
            Long saleId,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        List<DigitalTicket> tickets =
                ticketRepository.findAllBySale_IdOrderBySequenceNumberAsc(
                        saleId);
        tickets.forEach(ticket -> ensureCanView(ticket, actor));
        return tickets.stream().map(this::toListItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public List<EventReservationOption> findVisibleEvents(
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        List<Event> events = actor.getRole().getName() == RoleName.ORGANIZER
                ? eventRepository.findAllByOrganizer_IdOrderByStartAtDesc(
                        actor.getId())
                : eventRepository.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "startAt"));
        return events.stream()
                .map(event -> new EventReservationOption(
                        event.getId(),
                        event.getTitle(),
                        event.getStartAt(),
                        event.getCapacity()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public byte[] createPdf(Long id, String authenticatedLogin) {
        return documentService.createPdf(
                authorizedTicket(id, authenticatedLogin));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public byte[] createQrPng(Long id, String authenticatedLogin) {
        return documentService.createQrPng(
                authorizedTicket(id, authenticatedLogin));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public String createGoogleWalletUrl(
            Long id,
            String authenticatedLogin) {
        return googleWalletService.createSaveUrl(
                authorizedTicket(id, authenticatedLogin));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'OPERATOR', 'ORGANIZER')")
    public byte[] createAppleWalletPass(
            Long id,
            String authenticatedLogin) {
        return appleWalletService.createPass(
                authorizedTicket(id, authenticatedLogin));
    }

    private DigitalTicket authorizedTicket(
            Long id,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        DigitalTicket ticket = findTicket(id);
        ensureCanView(ticket, actor);
        return ticket;
    }

    private DigitalTicket findTicket(Long id) {
        return ticketRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la boleta solicitada."));
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private void ensureCanView(DigitalTicket ticket, User actor) {
        RoleName role = actor.getRole().getName();
        if (role == RoleName.ADMINISTRATOR || role == RoleName.OPERATOR) {
            return;
        }
        if (role == RoleName.ORGANIZER
                && ticket.getEvent().getOrganizer().getId()
                        .equals(actor.getId())) {
            return;
        }
        throw new BusinessRuleException(
                "No tienes permiso para consultar esta boleta.");
    }

    private Long organizerFilter(User actor) {
        return actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;
    }

    private TicketListItem toListItem(DigitalTicket ticket) {
        return new TicketListItem(
                ticket.getId(),
                ticket.getUniqueCode(),
                ticket.getSale().getReferenceCode(),
                ticket.getEvent().getId(),
                ticket.getEvent().getTitle(),
                ticket.getAttendeeName(),
                ticket.getAttendeeEmail(),
                ticket.getTicketTypeName(),
                ticket.getStatus(),
                ticket.getIssuedAt(),
                ticket.getUsedAt());
    }

    private TicketDetailsView toDetailsView(DigitalTicket ticket) {
        return new TicketDetailsView(
                ticket.getId(),
                ticket.getUniqueCode(),
                ticket.getSale().getId(),
                ticket.getSale().getReferenceCode(),
                ticket.getEvent().getId(),
                ticket.getEvent().getTitle(),
                ticket.getEvent().getStartAt(),
                ticket.getEvent().getEndAt(),
                ticket.getEvent().getVenue(),
                ticket.getEvent().getAddress(),
                ticket.getEvent().getOrganizer().getFullName(),
                ticket.getAttendeeName(),
                ticket.getAttendeeEmail(),
                ticket.getTicketTypeName(),
                ticket.getZone(),
                ticket.getSeat(),
                ticket.getStatus(),
                ticket.getAntiFraudCode(),
                ticket.getSignatureKeyId(),
                ticket.getSignedPayloadHash().substring(0, 16),
                ticket.getIssuedAt(),
                ticket.getUsedAt(),
                ticket.getCancelledAt(),
                ticket.getCancellationReason(),
                googleWalletService.isAvailable(),
                appleWalletService.isAvailable());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
