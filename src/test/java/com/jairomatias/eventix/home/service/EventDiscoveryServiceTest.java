package com.jairomatias.eventix.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventDiscoveryServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new EventDiscoveryService(eventRepository);
    }

    @Test
    void shouldExposeUpcomingPublishedEventsAsHomeViews() {
        Event event = event(11L, "Festival Eventix", "Música");
        when(eventRepository.findAllByStatusAndStartAtAfterOrderByStartAtAsc(
                eq(EventStatus.PUBLISHED),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(List.of(event));

        var result = service.upcomingEvents();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(11L);
        assertThat(result.getFirst().title()).isEqualTo("Festival Eventix");
        assertThat(result.getFirst().categoryName()).isEqualTo("Música");
    }

    @Test
    void shouldExposeTheNextSevenPublishedEventsRegardlessOfCalendarDay() {
        List<Event> events = List.of(
                event(1L, "Evento 1", "Música"),
                event(2L, "Evento 2", "Teatro"),
                event(3L, "Evento 3", "Deportes"),
                event(4L, "Evento 4", "Cultura"),
                event(5L, "Evento 5", "Comedia"),
                event(6L, "Evento 6", "Concierto"),
                event(7L, "Evento 7", "Festival"));
        when(eventRepository.findAllByStatusAndStartAtAfterOrderByStartAtAsc(
                eq(EventStatus.PUBLISHED),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(events);

        var result = service.nextSevenEvents();

        assertThat(result).hasSize(7);
        assertThat(result).extracting(view -> view.id())
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    private Event event(Long id, String title, String categoryName) {
        Event event = mock(Event.class);
        EventCategory category = mock(EventCategory.class);
        when(event.getId()).thenReturn(id);
        when(event.getTitle()).thenReturn(title);
        when(event.getCategory()).thenReturn(category);
        when(category.getName()).thenReturn(categoryName);
        when(event.getStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(event.getVenue()).thenReturn("Santo Domingo");
        when(event.getCoverImageUrl()).thenReturn("/events/media/cover.webp");
        when(event.isFreeEvent()).thenReturn(false);
        when(event.getBasePrice()).thenReturn(new BigDecimal("1500.00"));
        return event;
    }
}
