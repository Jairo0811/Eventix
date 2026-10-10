package com.jairomatias.eventix.venue.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
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
        return loadInventory(eventId, null);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public List<EventSeatView> getInventory(Long eventId, Long requiredSectionId) {
        requireSection(requiredSectionId);
        return loadInventory(eventId, requiredSectionId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public Optional<SeatHoldResult> getActiveHold(
            Long eventId,
            String authenticatedLogin) {
        ensureEventExists(eventId);
        User buyer = findActor(authenticatedLogin);
        LocalDateTime now = now();
        inventoryRepository.releaseExpiredHolds(now);

        List<EventSeatInventory> held = inventoryRepository
                .findHeldByUserForUpdate(eventId, buyer.getId());
        if (held.isEmpty()) {
            return Optional.empty();
        }

        String token = held.get(0).getHoldToken();
        if (held.stream().anyMatch(item -> !token.equals(item.getHoldToken()))) {
            throw new BusinessRuleException(
                    "Tu cuenta tiene más de una retención activa para este evento. Libéralas antes de continuar.");
        }

        LocalDateTime expiresAt = held.stream()
                .map(EventSeatInventory::getHoldExpiresAt)
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        return Optional.of(new SeatHoldResult(
                token,
                expiresAt,
                held.stream().map(item -> item.getSeat().getId()).toList()));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public SeatHoldResult holdSeats(
            Long eventId,
            Collection<Long> seatIds,
            Long requiredSectionId,
            String authenticatedLogin) {
        requireSection(requiredSectionId);
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessRuleException("Selecciona al menos un asiento.");
        }

        List<Long> requested = seatIds.stream().distinct().toList();
        if (requested.size() > 10) {
            throw new BusinessRuleException(
                    "Puedes retener entre 1 y 10 asientos por operación.");
        }

        lockEvent(eventId);
        User buyer = findActor(authenticatedLogin);
        LocalDateTime now = now();
        inventoryRepository.releaseExpiredHolds(now);
        ensureNoActiveHold(eventId, buyer);

        List<EventSeatInventory> inventory = inventoryRepository.findForUpdate(eventId, requested);

        if (inventory.size() != requested.size()) {
            throw new BusinessRuleException(
                    "Uno o más asientos no pertenecen al inventario del evento.");
        }

        for (EventSeatInventory item : inventory) {
            if (!requiredSectionId.equals(sectionId(item))) {
                throw new BusinessRuleException(
                        "Todos los asientos deben pertenecer a la sección asignada al tipo de entrada.");
            }
            if (item.getStatus() != EventSeatStatus.AVAILABLE) {
                throw new BusinessRuleException(
                        "El asiento " + item.getSeat().getLabel() + " ya no está disponible.");
            }
        }

        return createHold(inventory, now, buyer);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public SeatHoldResult holdBestAvailableSeats(
            Long eventId,
            int quantity,
            boolean accessibilityRequired,
            Long requiredSectionId,
            String authenticatedLogin) {
        requireSection(requiredSectionId);
        if (quantity < 1 || quantity > 10) {
            throw new BusinessRuleException(
                    "Puedes solicitar entre 1 y 10 asientos por operación.");
        }

        lockEvent(eventId);
        User buyer = findActor(authenticatedLogin);
        LocalDateTime now = now();
        inventoryRepository.releaseExpiredHolds(now);
        ensureNoActiveHold(eventId, buyer);

        List<EventSeatInventory> inventory = inventoryRepository
                .findAllForBestAvailableForUpdate(eventId)
                .stream()
                .filter(item -> requiredSectionId.equals(sectionId(item)))
                .toList();

        if (inventory.isEmpty()) {
            throw new BusinessRuleException(
                    "La sección seleccionada no tiene inventario de asientos reservados disponible.");
        }

        Map<Long, List<EventSeatInventory>> rows = new LinkedHashMap<>();
        for (EventSeatInventory item : inventory) {
            Long rowId = item.getSeat().getRow().getId();
            rows.computeIfAbsent(rowId, ignored -> new ArrayList<>()).add(item);
        }

        for (List<EventSeatInventory> row : rows.values()) {
            row.sort(this::compareSeatPosition);
            List<EventSeatInventory> block = findBestContiguousBlock(
                    row,
                    quantity,
                    accessibilityRequired);
            if (!block.isEmpty()) {
                return createHold(block, now, buyer);
            }
        }

        if (accessibilityRequired) {
            throw new BusinessRuleException(
                    "No hay un bloque accesible contiguo con la cantidad solicitada en esta sección.");
        }
        throw new BusinessRuleException(
                "No hay un bloque contiguo de asientos estándar con la cantidad solicitada en esta sección.");
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public int validateActiveHold(
            Long eventId,
            String holdToken,
            String authenticatedLogin) {
        return validateActiveHold(eventId, holdToken, null, authenticatedLogin);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public int validateActiveHold(
            Long eventId,
            String holdToken,
            Long requiredSectionId,
            String authenticatedLogin) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new BusinessRuleException(
                    "Selecciona y retén tus asientos antes de continuar.");
        }

        User buyer = findActor(authenticatedLogin);
        LocalDateTime now = now();
        inventoryRepository.releaseExpiredHolds(now);
        List<EventSeatInventory> held = inventoryRepository
                .findHeldByTokenForUpdate(eventId, holdToken);

        if (held.isEmpty()) {
            throw new BusinessRuleException(
                    "La retención de asientos no existe o ya expiró.");
        }

        for (EventSeatInventory item : held) {
            assertHoldOwnedBy(item, buyer);
            if (item.getStatus() != EventSeatStatus.HELD
                    || !holdToken.equals(item.getHoldToken())) {
                throw new BusinessRuleException(
                        "La retención de asientos ya no es válida.");
            }
            if (requiredSectionId != null
                    && !requiredSectionId.equals(sectionId(item))) {
                throw new BusinessRuleException(
                        "Los asientos retenidos no pertenecen a la sección del tipo de entrada seleccionado.");
            }
        }

        return held.size();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void releaseHold(
            Long eventId,
            String holdToken,
            String authenticatedLogin) {
        if (holdToken == null || holdToken.isBlank()) {
            return;
        }

        User buyer = findActor(authenticatedLogin);
        List<EventSeatInventory> held = inventoryRepository
                .findHeldByTokenForUpdate(eventId, holdToken);

        for (EventSeatInventory item : held) {
            assertHoldOwnedBy(item, buyer);
        }
        held.stream()
                .filter(item -> item.getStatus() == EventSeatStatus.HELD)
                .forEach(EventSeatInventory::release);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void confirmSale(
            Long eventId,
            String holdToken,
            Long saleId,
            String authenticatedLogin) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new BusinessRuleException("El hold de asientos es obligatorio.");
        }

        User buyer = findActor(authenticatedLogin);
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta solicitada."));

        if (!sale.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException(
                    "La venta no pertenece al evento indicado.");
        }
        if (sale.getSoldBy() == null || !buyer.getId().equals(sale.getSoldBy().getId())) {
            throw new BusinessRuleException(
                    "La venta no pertenece al usuario que retuvo los asientos.");
        }

        LocalDateTime now = now();
        inventoryRepository.releaseExpiredHolds(now);
        List<EventSeatInventory> held = inventoryRepository
                .findHeldByTokenForUpdate(eventId, holdToken);

        if (held.isEmpty()) {
            throw new BusinessRuleException(
                    "El hold de asientos no existe o expiró antes de confirmar la venta.");
        }

        for (EventSeatInventory item : held) {
            assertHoldOwnedBy(item, buyer);
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

    private List<EventSeatView> loadInventory(Long eventId, Long requiredSectionId) {
        releaseExpiredHolds();
        ensureEventExists(eventId);

        return inventoryRepository
                .findAllByEvent_IdOrderBySeat_Row_Section_SortOrderAscSeat_Row_SortOrderAscSeat_SeatNumberAsc(eventId)
                .stream()
                .filter(item -> requiredSectionId == null
                        || requiredSectionId.equals(sectionId(item)))
                .map(this::toView)
                .toList();
    }

    private SeatHoldResult createHold(
            List<EventSeatInventory> inventory,
            LocalDateTime now,
            User buyer) {
        String holdToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = now.plus(DEFAULT_HOLD_DURATION);
        inventory.forEach(item -> item.hold(holdToken, expiresAt, buyer));

        return new SeatHoldResult(
                holdToken,
                expiresAt,
                inventory.stream().map(item -> item.getSeat().getId()).toList());
    }

    private void ensureNoActiveHold(Long eventId, User buyer) {
        List<EventSeatInventory> active = inventoryRepository
                .findHeldByUserForUpdate(eventId, buyer.getId());
        if (!active.isEmpty()) {
            throw new BusinessRuleException(
                    "Ya tienes una retención activa para este evento. Continúa con ella o libérala antes de seleccionar otros asientos.");
        }
    }

    private void assertHoldOwnedBy(EventSeatInventory item, User buyer) {
        if (!item.isHeldBy(buyer.getId())) {
            throw new BusinessRuleException(
                    "La retención de asientos pertenece a otro usuario.");
        }
    }

    private Long sectionId(EventSeatInventory item) {
        return item.getSeat().getRow().getSection().getId();
    }

    private void requireSection(Long requiredSectionId) {
        if (requiredSectionId == null) {
            throw new BusinessRuleException(
                    "Selecciona un tipo de entrada con sección reservada antes de elegir asientos.");
        }
    }

    private List<EventSeatInventory> findBestContiguousBlock(
            List<EventSeatInventory> row,
            int quantity,
            boolean accessibilityRequired) {
        if (row.size() < quantity) {
            return List.of();
        }

        double rowCenter = (row.size() - 1) / 2.0;
        double bestDistance = Double.MAX_VALUE;
        List<EventSeatInventory> best = List.of();

        for (int start = 0; start <= row.size() - quantity; start++) {
            List<EventSeatInventory> window = row.subList(start, start + quantity);
            if (!isAvailableBlock(window)
                    || !matchesAccessibility(window, accessibilityRequired)) {
                continue;
            }

            double windowCenter = start + (quantity - 1) / 2.0;
            double distance = Math.abs(windowCenter - rowCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = List.copyOf(window);
            }
        }

        return best;
    }

    private boolean isAvailableBlock(List<EventSeatInventory> window) {
        return window.stream()
                .allMatch(item -> item.getStatus() == EventSeatStatus.AVAILABLE);
    }

    private boolean matchesAccessibility(
            List<EventSeatInventory> window,
            boolean accessibilityRequired) {
        if (!accessibilityRequired) {
            return window.stream().allMatch(item ->
                    !item.getSeat().isAccessible()
                            && !item.getSeat().isCompanionSeat());
        }

        boolean hasAccessibleSeat = window.stream()
                .anyMatch(item -> item.getSeat().isAccessible());
        boolean accessibilityOnly = window.stream().allMatch(item ->
                item.getSeat().isAccessible()
                        || item.getSeat().isCompanionSeat());
        return hasAccessibleSeat && accessibilityOnly;
    }

    private int compareSeatPosition(
            EventSeatInventory left,
            EventSeatInventory right) {
        BigDecimal leftX = left.getSeat().getXPosition();
        BigDecimal rightX = right.getSeat().getXPosition();
        if (leftX != null || rightX != null) {
            int xComparison = Comparator.nullsLast(BigDecimal::compareTo)
                    .compare(leftX, rightX);
            if (xComparison != 0) {
                return xComparison;
            }
        }

        int numberComparison = compareSeatNumbers(
                left.getSeat().getSeatNumber(),
                right.getSeat().getSeatNumber());
        if (numberComparison != 0) {
            return numberComparison;
        }
        return Comparator.nullsLast(Long::compareTo)
                .compare(left.getSeat().getId(), right.getSeat().getId());
    }

    private int compareSeatNumbers(String left, String right) {
        if (left == null || right == null) {
            return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    .compare(left, right);
        }
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException ignored) {
            return left.compareToIgnoreCase(right);
        }
    }

    private Event lockEvent(Long eventId) {
        return eventRepository.findDetailedByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
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
