package com.jairomatias.eventix.venue.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.dto.EventSeatView;
import com.jairomatias.eventix.venue.dto.SeatHoldResult;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.repository.EventSeatInventoryRepository;
import com.jairomatias.eventix.venue.repository.VenueSeatRepository;

@Service
public class DefaultEventSeatInventoryService implements EventSeatInventoryService {

    private static final Duration DEFAULT_HOLD_DURATION = Duration.ofMinutes(10);

    private final EventRepository eventRepository;
    private final VenueSeatRepository seatRepository;
    private final EventSeatInventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public DefaultEventSeatInventoryService(
            EventRepository eventRepository,
            VenueSeatRepository seatRepository,
            EventSeatInventoryRepository inventoryRepository,
            SaleRepository saleRepository,
            UserRepository userRepository) {
        this(
                eventRepository,
                seatRepository,
                inventoryRepository,
                saleRepository,
                userRepository,
                Clock.systemDefaultZone());
    }

    DefaultEventSeatInventoryService(
            EventRepository eventRepository,
            VenueSeatRepository seatRepository,
            EventSeatInventoryRepository inventoryRepository,
            SaleRepository saleRepository,
            UserRepository userRepository,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.inventoryRepository = inventoryRepository;
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public int initializeInventory(Long eventId, String authenticatedLogin) {
        Event event = eventRepository.findDetailedByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
        ensureCanManage(event, findActor(authenticatedLogin));

        if (event.getVenueDefinition() == null) {
            throw new BusinessRuleException(
                    "El evento debe tener un recinto estructurado antes de crear inventario.");
        }

        if (event.getSeatingMode() == EventSeatingMode.GENERAL_ADMISSION) {
            throw new BusinessRuleException(
                    "El evento está configurado como admisión general.");
        }

        if (inventoryRepository.existsByEvent_Id(eventId)) {
            throw new BusinessRuleException(
                    "El inventario de asientos ya fue inicializado para este evento.");
        }

        List<VenueSeat> seats = seatRepository.findActiveReservedSeatsByVenueId(
                event.getVenueDefinition().getId());

        if (seats.isEmpty()) {
            throw new BusinessRuleException(
                    "El recinto no tiene asientos reservados activos para este evento.");
        }

        List<EventSeatInventory> inventory = seats.stream()
                .map(seat -> new EventSeatInventory(event, seat))
                .toList();

        inventoryRepository.saveAll(inventory);
        return inventory.size();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public List<EventSeatView> getInventory(Long eventId) {
        releaseExpiredHolds();
        ensureEventExists(eventId);

        return inventoryRepository
                .findAllByEvent_IdOrderBySeat_Row_Section_SortOrderAscSeat_Row_SortOrderAscSeat_SeatNumberAsc(eventId)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public SeatHoldResult holdSeats(Long eventId, Collection<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessRuleException("Selecciona al menos un asiento.");
        }

        List<Long> requested = seatIds.stream().distinct().toList();
        LocalDateTime now = now();

        List<EventSeatInventory> inventory = inventoryRepository.findForUpdate(eventId, requested);

        if (inventory.size() != requested.size()) {
            throw new BusinessRuleException(
                    "Uno o más asientos no pertenecen al inventario del evento.");
        }

        for (EventSeatInventory item : inventory) {
            if (item.isHeldAndExpired(now)) {
                item.release();
            }
            if (item.getStatus() != EventSeatStatus.AVAILABLE) {
                throw new BusinessRuleException(
                        "El asiento " + item.getSeat().getLabel() + " ya no está disponible.");
            }
        }

        String holdToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = now.plus(DEFAULT_HOLD_DURATION);

        inventory.forEach(item -> item.hold(holdToken, expiresAt));

        return new SeatHoldResult(
                holdToken,
                expiresAt,
                inventory.stream().map(item -> item.getSeat().getId()).toList());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void releaseHold(Long eventId, String holdToken) {
        if (holdToken == null || holdToken.isBlank()) {
            return;
        }

        List<EventSeatInventory> held = inventoryRepository
                .findHeldByTokenForUpdate(eventId, holdToken);

        for (EventSeatInventory item : held) {
            if (item.getStatus() == EventSeatStatus.HELD) {
                item.release();
            }
        }
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void confirmSale(Long eventId, String holdToken, Long saleId) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new BusinessRuleException("El hold de asientos es obligatorio.");
        }

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));

        if (!sale.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException(
                    "La venta no pertenece al evento indicado.");
        }

        LocalDateTime now = now();
        List<EventSeatInventory> held = inventoryRepository
                .findHeldByTokenForUpdate(eventId, holdToken);

        if (held.isEmpty()) {
            throw new BusinessRuleException(
                    "El hold de asientos no existe o ya fue liberado.");
        }

        for (EventSeatInventory item : held) {
            if (item.isHeldAndExpired(now)) {
                item.release();
                throw new BusinessRuleException(
                        "El hold de asientos expiró antes de confirmar la venta.");
            }

            if (item.getStatus() != EventSeatStatus.HELD
                    || !holdToken.equals(item.getHoldToken())) {
                throw new BusinessRuleException(
                        "El hold de asientos ya no es válido.");
            }
        }

        held.forEach(item -> item.sell(sale));
    }

    @Override
    @Transactional
    public int releaseExpiredHolds() {
        return inventoryRepository.releaseExpiredHolds(now());
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private void ensureCanManage(Event event, User actor) {
        if (actor.getRole().getName() == RoleName.ADMINISTRATOR) {
            return;
        }
        if (actor.getRole().getName() == RoleName.ORGANIZER
                && event.getOrganizer().getId().equals(actor.getId())) {
            return;
        }
        throw new BusinessRuleException(
                "No tienes permiso para administrar el inventario de este evento.");
    }

    private void ensureEventExists(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException(
                    "No se encontró el evento solicitado.");
        }
    }

    private EventSeatView toView(EventSeatInventory item) {
        return new EventSeatView(
                item.getId(),
                item.getSeat().getId(),
                item.getSeat().getRow().getSection().getCode(),
                item.getSeat().getRow().getSection().getName(),
                item.getSeat().getRow().getCode(),
                item.getSeat().getSeatNumber(),
                item.getSeat().getLabel(),
                item.getSeat().isAccessible(),
                item.getStatus(),
                item.getHoldExpiresAt());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
