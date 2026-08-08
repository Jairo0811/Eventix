package com.jairomatias.eventix.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.dto.EventForm;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.mapper.EventMapper;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DefaultEventServiceTest {

    private static final String ORGANIZER_LOGIN =
            "organizer@eventix.local";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 7, 30, 10, 0);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private User organizer;

    @Mock
    private Role organizerRole;

    @Mock
    private EventCategory category;

    private DefaultEventService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-30T14:00:00Z"),
                ZoneId.of("America/Santo_Domingo"));

        service = new DefaultEventService(
                eventRepository,
                categoryRepository,
                userRepository,
                eventMapper,
                reservationRepository,
                eventPublisher,
                clock);
    }

    @Test
    void organizerCreatesFreeDraftAssignedToOwnAccount() {
        prepareOrganizer();
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(category));
        when(category.isActive()).thenReturn(true);

        Event persisted = org.mockito.Mockito.mock(Event.class);
        when(persisted.getId()).thenReturn(42L);
        when(eventRepository.save(any(Event.class)))
                .thenReturn(persisted);

        EventForm form = validForm();
        form.setStatus(EventStatus.DRAFT);
        form.setFreeEvent(true);
        form.setBasePrice(new BigDecimal("950.00"));

        Long eventId = service.create(form, ORGANIZER_LOGIN);

        assertThat(eventId).isEqualTo(42L);

        ArgumentCaptor<Event> eventCaptor =
                ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event created = eventCaptor.getValue();
        assertThat(created.getOrganizer()).isSameAs(organizer);
        assertThat(created.isFreeEvent()).isTrue();
        assertThat(created.getBasePrice())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void eventEndMustBeAfterStart() {
        prepareOrganizer();
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(category));
        when(category.isActive()).thenReturn(true);

        EventForm form = validForm();
        form.setEndAt(form.getStartAt().minusMinutes(1));

        assertThatThrownBy(() ->
                service.create(form, ORGANIZER_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(
                        "finalización debe ser posterior");
    }

    @Test
    void organizerCannotDeleteAnotherOrganizersEvent() {
        prepareOrganizer();
        when(organizer.getId()).thenReturn(7L);

        User otherOrganizer =
                org.mockito.Mockito.mock(User.class);
        when(otherOrganizer.getId()).thenReturn(99L);

        Event event = org.mockito.Mockito.mock(Event.class);
        when(event.getOrganizer()).thenReturn(otherOrganizer);
        when(eventRepository.findDetailedByIdForUpdate(8L))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() ->
                service.delete(8L, ORGANIZER_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void paidEventRequiresPositivePrice() {
        prepareOrganizer();
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(category));
        when(category.isActive()).thenReturn(true);

        EventForm form = validForm();
        form.setFreeEvent(false);
        form.setBasePrice(BigDecimal.ZERO);

        assertThatThrownBy(() ->
                service.create(form, ORGANIZER_LOGIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("precio base mayor que cero");
    }

    private void prepareOrganizer() {
        when(userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        ORGANIZER_LOGIN,
                        ORGANIZER_LOGIN))
                .thenReturn(Optional.of(organizer));
        when(organizer.getRole()).thenReturn(organizerRole);
        when(organizerRole.getName())
                .thenReturn(RoleName.ORGANIZER);
    }

    private EventForm validForm() {
        EventForm form = new EventForm();
        form.setTitle("Conferencia Java Caribe");
        form.setDescription(
                "Encuentro para la comunidad de desarrollo.");
        form.setCategoryId(3L);
        form.setStatus(EventStatus.DRAFT);
        form.setStartAt(NOW.plusDays(2));
        form.setEndAt(NOW.plusDays(2).plusHours(3));
        form.setVenue("Centro de Convenciones");
        form.setAddress("Santo Domingo, República Dominicana");
        form.setCapacity(500);
        form.setOrganizerId(7L);
        form.setCoverImageUrl(
                "https://example.com/event-cover.jpg");
        form.setFreeEvent(true);
        form.setBasePrice(BigDecimal.ZERO);
        return form;
    }
}
