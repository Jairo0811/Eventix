package com.jairomatias.eventix.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.dto.ReservationForm;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;
import com.jairomatias.eventix.reservation.mapper.ReservationMapper;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DefaultReservationServiceTest {

    private static final String OPERATOR_LOGIN =
            "operator@eventix.local";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 1, 15, 0);

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ReservationReferenceGenerator referenceGenerator;

    @Mock
    private User operator;

    @Mock
    private Role operatorRole;

    @Mock
    private Event event;

    private DefaultReservationService service;

    @BeforeEach
    void setUp() {
        ReservationProperties properties = new ReservationProperties();
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-01T19:00:00Z"),
                ZoneId.of("America/Santo_Domingo"));

        service = new DefaultReservationService(
                reservationRepository,
                eventRepository,
                userRepository,
                reservationMapper,
                referenceGenerator,
                properties,
                clock);
    }

    @Test
    void createsPendingReservationWithConfiguredExpiration() {
        prepareOperator();
        preparePublishedEvent(100);
        when(reservationRepository.sumOccupiedSeats(8L, NOW))
                .thenReturn(92L);
        when(referenceGenerator.generate())
                .thenReturn("RES-ABCDEFGH2345");
        when(reservationRepository.existsByReferenceCode(
                "RES-ABCDEFGH2345"))
                .thenReturn(false);

        Reservation persisted = org.mockito.Mockito.mock(
                Reservation.class);
        when(persisted.getId()).thenReturn(55L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenReturn(persisted);

        Long reservationId = service.create(
                validForm(8),
                OPERATOR_LOGIN);

        assertThat(reservationId).isEqualTo(55L);

        ArgumentCaptor<Reservation> captor =
                ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        Reservation reservation = captor.getValue();
        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getAttendeeEmail())
                .isEqualTo("asistente@example.com");
        assertThat(reservation.getExpiresAt())
                .isEqualTo(NOW.plusMinutes(15));
    }

    @Test
    void rejectsReservationThatWouldOversellEvent() {
        prepareOperator();
        preparePublishedEvent(100);
        when(reservationRepository.sumOccupiedSeats(8L, NOW))
                .thenReturn(95L);

        assertThatThrownBy(() -> service.create(
                validForm(6),
                OPERATOR_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Disponibles: 5");
    }

    @Test
    void rejectsDuplicateActiveReservationForSameEmailAndEvent() {
        prepareOperator();
        preparePublishedEvent(100);
        when(reservationRepository.existsActiveDuplicate(
                8L,
                "asistente@example.com",
                NOW,
                null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                validForm(1),
                OPERATOR_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe una reservación activa");
    }

    @Test
    void expiredReservationCannotBeConfirmed() {
        prepareOperator();

        Reservation reservation = org.mockito.Mockito.mock(
                Reservation.class);
        when(reservation.getStatus())
                .thenReturn(ReservationStatus.PENDING);
        when(reservation.getExpiresAt())
                .thenReturn(NOW.minusSeconds(1));
        when(reservationRepository.findEventIdById(9L))
                .thenReturn(Optional.of(8L));
        when(eventRepository.findDetailedByIdForUpdate(8L))
                .thenReturn(Optional.of(event));
        when(reservationRepository.findDetailedByIdForUpdate(9L))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.confirm(
                9L,
                OPERATOR_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expiró");
    }

    private void prepareOperator() {
        when(userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        OPERATOR_LOGIN,
                        OPERATOR_LOGIN))
                .thenReturn(Optional.of(operator));
        when(operator.getRole()).thenReturn(operatorRole);
        when(operatorRole.getName()).thenReturn(RoleName.OPERATOR);
    }

    private void preparePublishedEvent(int capacity) {
        when(eventRepository.findDetailedByIdForUpdate(8L))
                .thenReturn(Optional.of(event));
        when(event.getId()).thenReturn(8L);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getStartAt()).thenReturn(NOW.plusDays(2));
        org.mockito.Mockito.lenient()
                .when(event.getCapacity())
                .thenReturn(capacity);
    }

    private ReservationForm validForm(int quantity) {
        ReservationForm form = new ReservationForm();
        form.setEventId(8L);
        form.setAttendeeFirstName("María");
        form.setAttendeeLastName("Pérez");
        form.setAttendeeEmail("ASISTENTE@EXAMPLE.COM");
        form.setAttendeePhone("809-555-0101");
        form.setQuantity(quantity);
        return form;
    }
}
