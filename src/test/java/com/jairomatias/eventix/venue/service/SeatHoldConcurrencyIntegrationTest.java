package com.jairomatias.eventix.venue.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.role.repository.RoleRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.entity.EventSeatInventory;
import com.jairomatias.eventix.venue.entity.EventSeatStatus;
import com.jairomatias.eventix.venue.entity.Venue;
import com.jairomatias.eventix.venue.entity.VenueRow;
import com.jairomatias.eventix.venue.entity.VenueSeat;
import com.jairomatias.eventix.venue.entity.VenueSection;
import com.jairomatias.eventix.venue.entity.VenueSectionType;
import com.jairomatias.eventix.venue.repository.EventSeatInventoryRepository;
import com.jairomatias.eventix.venue.repository.VenueRepository;
import com.jairomatias.eventix.venue.repository.VenueRowRepository;
import com.jairomatias.eventix.venue.repository.VenueSeatRepository;
import com.jairomatias.eventix.venue.repository.VenueSectionRepository;

/**
 * Comprueba, contra la base de datos real y en transacciones separadas, que dos
 * compradores que piden el mismo asiento a la vez no acaban ambos con una
 * retención: el bloqueo pesimista debe dejar exactamente un ganador.
 *
 * <p>La prueba no usa la gestión transaccional de JUnit a propósito. Los hilos
 * necesitan ver datos confirmados, así que el montaje se confirma con
 * {@link TransactionTemplate} y se limpia explícitamente al terminar.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeatHoldConcurrencyIntegrationTest {

    private static final String FIXTURE_TAG = "concurrency-seat-hold";
    private static final String FIRST_BUYER = "concurrency.buyer.one@eventix.local";
    private static final String SECOND_BUYER = "concurrency.buyer.two@eventix.local";

    @Autowired private EventSeatInventoryService inventoryService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventCategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private VenueSectionRepository sectionRepository;
    @Autowired private VenueRowRepository rowRepository;
    @Autowired private VenueSeatRepository seatRepository;
    @Autowired private EventSeatInventoryRepository inventoryRepository;

    private Long eventId;
    private Long sectionId;
    private Long seatId;
    private Long venueId;

    @BeforeEach
    void createCommittedFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            User organizer = saveUser(
                    "Organizador",
                    "Concurrencia",
                    FIXTURE_TAG + ".organizer@eventix.local",
                    RoleName.ORGANIZER);
            saveUser("Primer", "Comprador", FIRST_BUYER, RoleName.USER);
            saveUser("Segundo", "Comprador", SECOND_BUYER, RoleName.USER);

            EventCategory category = categoryRepository.save(
                    new EventCategory(FIXTURE_TAG + " categoría", "Fixture de concurrencia"));

            Venue venue = venueRepository.save(new Venue(
                    FIXTURE_TAG + " recinto",
                    "Calle de prueba 1",
                    "Santo Domingo",
                    "DO",
                    "America/Santo_Domingo"));
            VenueSection section = sectionRepository.save(new VenueSection(
                    venue,
                    FIXTURE_TAG + "-S1",
                    "Sección de prueba",
                    VenueSectionType.RESERVED_SEATING,
                    1,
                    1));
            VenueRow row = rowRepository.save(new VenueRow(section, "F1", "Fila 1", 1));
            VenueSeat seat = seatRepository.save(new VenueSeat(row, "1", "F1-1", false, false));

            Event event = new Event(
                    FIXTURE_TAG + " evento",
                    "Evento creado para la prueba de concurrencia",
                    category,
                    EventStatus.PUBLISHED,
                    LocalDateTime.now().plusDays(30),
                    LocalDateTime.now().plusDays(30).plusHours(3),
                    venue.getName(),
                    venue.getAddress(),
                    1,
                    organizer,
                    null,
                    false,
                    new BigDecimal("1000.00"));
            event.configureVenue(venue, EventSeatingMode.RESERVED_SEATING);
            Event savedEvent = eventRepository.save(event);

            inventoryRepository.save(new EventSeatInventory(savedEvent, seat));

            this.eventId = savedEvent.getId();
            this.sectionId = section.getId();
            this.seatId = seat.getId();
            this.venueId = venue.getId();
        });
    }

    @Test
    void twoBuyersRequestingTheSameSeatMustProduceExactlyOneHold() throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Outcome> first = pool.submit(holdAttempt(FIRST_BUYER, startTogether));
            Future<Outcome> second = pool.submit(holdAttempt(SECOND_BUYER, startTogether));

            Outcome firstOutcome = first.get(60, TimeUnit.SECONDS);
            Outcome secondOutcome = second.get(60, TimeUnit.SECONDS);

            long winners = countWinners(firstOutcome, secondOutcome);
            assertThat(winners)
                    .as("Exactamente un comprador debe retener el asiento: %s / %s",
                            firstOutcome, secondOutcome)
                    .isEqualTo(1);

            assertThat(firstOutcome.failure() == null && secondOutcome.failure() == null)
                    .as("El perdedor debe recibir un error de negocio, no un éxito silencioso")
                    .isFalse();
        } finally {
            pool.shutdownNow();
        }

        EventSeatInventory persisted = inventoryRepository.findById(inventoryId()).orElseThrow();
        assertThat(persisted.getStatus())
                .as("El asiento debe quedar retenido una sola vez en la base de datos")
                .isEqualTo(EventSeatStatus.HELD);
    }

    private Callable<Outcome> holdAttempt(String login, CyclicBarrier barrier) {
        return () -> {
            authenticateAs(login);
            try {
                barrier.await(30, TimeUnit.SECONDS);
                inventoryService.holdSeats(eventId, List.of(seatId), sectionId, login);
                return new Outcome(login, null);
            } catch (Exception exception) {
                return new Outcome(login, exception);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private static void authenticateAs(String login) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        login,
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static long countWinners(Outcome... outcomes) {
        long winners = 0;
        for (Outcome outcome : outcomes) {
            if (outcome.failure() == null) {
                winners++;
            }
        }
        return winners;
    }

    private Long inventoryId() {
        AtomicReference<Long> id = new AtomicReference<>();
        transactionTemplate.executeWithoutResult(status ->
                id.set(inventoryRepository.findAll().stream()
                        .filter(item -> eventId.equals(item.getEvent().getId()))
                        .map(EventSeatInventory::getId)
                        .findFirst()
                        .orElseThrow()));
        return id.get();
    }

    private User saveUser(String firstName, String lastName, String email, RoleName roleName) {
        return userRepository.save(new User(
                firstName,
                lastName,
                email,
                email,
                "{noop}fixture-not-used-for-login",
                "8090000000",
                roleRepository.findByName(roleName).orElseThrow()));
    }

    @AfterEach
    void removeCommittedFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            inventoryRepository.deleteAll(
                    inventoryRepository.findAll().stream()
                            .filter(item -> eventId.equals(item.getEvent().getId()))
                            .toList());
            eventRepository.findById(eventId).ifPresent(eventRepository::delete);
            seatRepository.findById(seatId).ifPresent(seatRepository::delete);
            rowRepository.findAll().stream()
                    .filter(row -> row.getSection().getVenue().getId().equals(venueId))
                    .forEach(rowRepository::delete);
            sectionRepository.findById(sectionId).ifPresent(sectionRepository::delete);
            venueRepository.findById(venueId).ifPresent(venueRepository::delete);
            userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(FIRST_BUYER, FIRST_BUYER)
                    .ifPresent(userRepository::delete);
            userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(SECOND_BUYER, SECOND_BUYER)
                    .ifPresent(userRepository::delete);
        });
    }

    private record Outcome(String login, Exception failure) {

        @Override
        public String toString() {
            return login + (failure == null ? " -> retuvo" : " -> " + failure.getMessage());
        }
    }
}
