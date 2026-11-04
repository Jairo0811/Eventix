package com.jairomatias.eventix.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
class PublicStorefrontServiceTest {

    // Miércoles 4 de noviembre de 2026, 10:00
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventCategoryRepository categoryRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private SaleItemRepository saleItemRepository;

    private PublicStorefrontService service() {
        return new PublicStorefrontService(
                eventRepository, categoryRepository, ticketTypeRepository, saleItemRepository);
    }

    @Test
    void todayRangeEndsAtMidnightAndStartsNow() {
        LocalDateTime[] range = PublicStorefrontService.dateRange("today", NOW);

        assertThat(range[0]).isEqualTo(NOW);
        assertThat(range[1]).isEqualTo(LocalDateTime.of(2026, 11, 5, 0, 0));
    }

    @Test
    void weekendRangeFromMidweekRunsFridayEveningToMondayMidnight() {
        LocalDateTime[] range = PublicStorefrontService.dateRange("weekend", NOW);

        assertThat(range[0]).isEqualTo(LocalDateTime.of(2026, 11, 6, 18, 0));
        assertThat(range[1]).isEqualTo(LocalDateTime.of(2026, 11, 9, 0, 0));
    }

    @Test
    void weekendRangeOnSundayNeverStartsInThePast() {
        LocalDateTime sunday = LocalDateTime.of(2026, 11, 8, 15, 0);

        LocalDateTime[] range = PublicStorefrontService.dateRange("weekend", sunday);

        assertThat(range[0]).isEqualTo(sunday);
        assertThat(range[1]).isEqualTo(LocalDateTime.of(2026, 11, 9, 0, 0));
    }

    @Test
    void monthRangeEndsAtTheFirstOfNextMonth() {
        LocalDateTime[] range = PublicStorefrontService.dateRange("month", NOW);

        assertThat(range[1]).isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0));
    }

    @Test
    void unknownFilterFallsBackToAllUpcomingEvents() {
        LocalDateTime[] range = PublicStorefrontService.dateRange("lo-que-sea", NOW);

        assertThat(range[0]).isEqualTo(NOW);
        assertThat(range[1]).isAfter(NOW.plusYears(5));
    }

    @Test
    void onlyHttpsMapLinksAreKept() {
        assertThat(PublicStorefrontService.safeHttpsUrl("https://maps.google.com/?q=1"))
                .isEqualTo("https://maps.google.com/?q=1");
        assertThat(PublicStorefrontService.safeHttpsUrl("HTTPS://maps.google.com")).isNotNull();
        assertThat(PublicStorefrontService.safeHttpsUrl("javascript:alert(1)")).isNull();
        assertThat(PublicStorefrontService.safeHttpsUrl("http://insecure.example")).isNull();
        assertThat(PublicStorefrontService.safeHttpsUrl(null)).isNull();
    }

    @Test
    void unpublishedEventsLookLikeTheyDoNotExist() {
        Event draft = mock(Event.class);
        when(draft.getStatus()).thenReturn(EventStatus.DRAFT);
        when(eventRepository.findDetailedById(5L)).thenReturn(Optional.of(draft));

        assertThat(service().findEvent(5L)).isEmpty();
    }

    @Test
    void missingEventsAreEmpty() {
        when(eventRepository.findDetailedById(9L)).thenReturn(Optional.empty());

        assertThat(service().findEvent(9L)).isEmpty();
    }
}
