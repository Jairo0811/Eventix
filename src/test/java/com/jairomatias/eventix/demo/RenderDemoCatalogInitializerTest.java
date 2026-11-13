package com.jairomatias.eventix.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.role.repository.RoleRepository;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RenderDemoCatalogInitializerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-13T12:00:00Z"),
            ZoneId.of("America/Santo_Domingo"));

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventCategoryRepository categoryRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private RenderDemoCatalogInitializer initializer(String dataSource) {
        return new RenderDemoCatalogInitializer(
                roleRepository, userRepository, categoryRepository, eventRepository,
                ticketTypeRepository, passwordEncoder, CLOCK, dataSource);
    }

    @Test
    void isOnlyRegisteredInRenderProfileAndExposesFictionalMode() {
        assertThat(RenderDemoCatalogInitializer.class.getAnnotation(Profile.class).value())
                .containsExactly("render");
        assertThat(RenderDemoModeAdvice.class.getAnnotation(Profile.class).value())
                .containsExactly("render");
        assertThat(new RenderDemoModeAdvice().demoMode()).isTrue();
    }

    @Test
    void refusesSeedingIntoPersistentOrCommercialDatabases() {
        assertThatThrownBy(() -> initializer(
                "jdbc:sqlserver://sqlserver:1433;databaseName=EventixDb")
                .run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("H2 en memoria");
        verifyNoInteractions(roleRepository, userRepository, categoryRepository,
                eventRepository, ticketTypeRepository, passwordEncoder);
    }

    @Test
    void doesNotDuplicateAnExistingDemoCatalog() throws Exception {
        when(eventRepository.count()).thenReturn(3L);
        initializer("jdbc:h2:mem:eventix;MODE=MSSQLServer")
                .run(new DefaultApplicationArguments(new String[0]));
        verify(eventRepository).count();
        verify(eventRepository, never()).save(any(Event.class));
        verifyNoInteractions(roleRepository, userRepository, categoryRepository,
                ticketTypeRepository, passwordEncoder);
    }

    @Test
    void createsExactlyThreeFictionalPublishedEventsAndNoAdministratorLogin() throws Exception {
        when(eventRepository.count()).thenReturn(0L);
        when(roleRepository.findByName(any(RoleName.class))).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(call -> call.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(categoryRepository.save(any(EventCategory.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketTypeRepository.save(any(TicketType.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-random-value");

        initializer("jdbc:h2:mem:eventix;DB_CLOSE_DELAY=-1")
                .run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> organizerCapture = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(organizerCapture.capture());
        User organizer = organizerCapture.getValue();
        assertThat(organizer.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(organizer.getEmail()).endsWith("@eventix.local");
        assertThat(organizer.getRole().getName()).isEqualTo(RoleName.ORGANIZER);
        assertThat(organizer.getPasswordHash()).isEqualTo("hashed-random-value");

        ArgumentCaptor<Event> eventCapture = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(3)).save(eventCapture.capture());
        List<Event> events = eventCapture.getAllValues();
        assertThat(events).extracting(Event::getStatus)
                .containsOnly(EventStatus.PUBLISHED);
        assertThat(events).allMatch(event -> event.getStartAt()
                .isAfter(LocalDateTime.now(CLOCK)));
        assertThat(events).allMatch(event -> event.getTitle().contains("DEMO"));
        assertThat(events).allMatch(event -> event.getAddress().contains("ficticio"));
        assertThat(events).filteredOn(Event::isFreeEvent).hasSize(1);
        assertThat(events).filteredOn(event -> !event.isFreeEvent()).hasSize(2);

        verify(roleRepository, times(RoleName.values().length)).save(any(Role.class));
        verify(categoryRepository, times(3)).save(any(EventCategory.class));
        verify(ticketTypeRepository, times(3)).save(any(TicketType.class));
    }
}
