package com.jairomatias.eventix.home.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.home.dto.HomeEventView;
import com.jairomatias.eventix.home.dto.PublicCategory;
import com.jairomatias.eventix.home.dto.PublicEventDetail;
import com.jairomatias.eventix.home.dto.PublicTicketOption;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.repository.SaleItemRepository;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;

/**
 * Consultas de solo lectura para la tienda pública (sin sesión): búsqueda de
 * eventos publicados y la ficha pública de cada evento.
 */
@Service
public class PublicStorefrontService {

    public static final int PAGE_SIZE = 12;
    private static final int MAX_TERM_LENGTH = 80;
    private static final int SITEMAP_LIMIT = 500;
    private static final int FAR_FUTURE_YEARS = 10;
    private static final int WEEKEND_START_HOUR = 18;

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SaleItemRepository saleItemRepository;

    public PublicStorefrontService(
            EventRepository eventRepository,
            EventCategoryRepository categoryRepository,
            TicketTypeRepository ticketTypeRepository,
            SaleItemRepository saleItemRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.saleItemRepository = saleItemRepository;
    }

    /** Filtros de fecha admitidos: all, today, weekend, month. */
    @Transactional(readOnly = true)
    public Page<HomeEventView> search(String term, Long categoryId, String when, int page) {
        return search(term, categoryId, when, page, LocalDateTime.now(PublicEventDetail.EVENT_ZONE));
    }

    Page<HomeEventView> search(
            String term, Long categoryId, String when, int page, LocalDateTime now) {
        LocalDateTime[] range = dateRange(when, now);
        return eventRepository.searchPublished(
                EventStatus.PUBLISHED,
                range[0],
                range[1],
                normalizeTerm(term),
                categoryId,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public List<PublicCategory> categories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(category -> new PublicCategory(category.getId(), category.getName()))
                .toList();
    }

    /** Solo eventos publicados; el resto responde como si no existiera. */
    @Transactional(readOnly = true)
    public Optional<PublicEventDetail> findEvent(Long id) {
        return eventRepository.findDetailedById(id)
                .filter(event -> event.getStatus() == EventStatus.PUBLISHED)
                .map(this::toDetail);
    }

    @Transactional(readOnly = true)
    public List<HomeEventView> sitemapEvents() {
        return eventRepository.findAllByStatusAndStartAtAfterOrderByStartAtAsc(
                EventStatus.PUBLISHED,
                LocalDateTime.now(PublicEventDetail.EVENT_ZONE),
                PageRequest.of(0, SITEMAP_LIMIT))
                .stream()
                .map(this::toView)
                .toList();
    }

    static LocalDateTime[] dateRange(String when, LocalDateTime now) {
        String filter = when == null ? "all" : when.toLowerCase(Locale.ROOT);
        LocalDate today = now.toLocalDate();
        return switch (filter) {
            case "today" -> new LocalDateTime[] {now, today.plusDays(1).atStartOfDay()};
            case "weekend" -> {
                LocalDate friday = switch (today.getDayOfWeek()) {
                    case SATURDAY -> today.minusDays(1);
                    case SUNDAY -> today.minusDays(2);
                    default -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
                };
                LocalDateTime start = friday.atTime(WEEKEND_START_HOUR, 0);
                yield new LocalDateTime[] {
                    start.isBefore(now) ? now : start,
                    friday.plusDays(3).atStartOfDay()};
            }
            case "month" -> new LocalDateTime[] {
                now, today.withDayOfMonth(1).plusMonths(1).atStartOfDay()};
            default -> new LocalDateTime[] {now, now.plusYears(FAR_FUTURE_YEARS)};
        };
    }

    private static String normalizeTerm(String term) {
        if (term == null) {
            return "";
        }
        String clean = term.trim();
        return clean.length() > MAX_TERM_LENGTH ? clean.substring(0, MAX_TERM_LENGTH) : clean;
    }

    private HomeEventView toView(Event event) {
        return new HomeEventView(
                event.getId(),
                event.getTitle(),
                event.getCategory().getName(),
                event.getStartAt(),
                event.getVenue(),
                event.getCoverImageUrl(),
                event.isFreeEvent(),
                event.getBasePrice());
    }

    private PublicEventDetail toDetail(Event event) {
        List<PublicTicketOption> tickets = ticketTypeRepository
                .findAllByEvent_IdAndActiveTrueOrderByNameAsc(event.getId())
                .stream()
                .map(this::toOption)
                .toList();
        boolean upcoming = event.getStartAt().isAfter(LocalDateTime.now(PublicEventDetail.EVENT_ZONE));
        boolean onSale = upcoming && tickets.stream().anyMatch(ticket -> !ticket.soldOut());
        return new PublicEventDetail(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory().getName(),
                event.getStartAt(),
                event.getEndAt(),
                event.getVenue(),
                event.getAddress(),
                safeHttpsUrl(event.getGoogleMapsUrl()),
                event.getCoverImageUrl(),
                event.isFreeEvent(),
                event.getBasePrice(),
                onSale,
                tickets);
    }

    private PublicTicketOption toOption(TicketType ticketType) {
        int allocated = Math.toIntExact(saleItemRepository.sumAllocatedQuantity(ticketType.getId()));
        return new PublicTicketOption(
                ticketType.getName(),
                ticketType.getCategory().getDisplayName(),
                ticketType.getPrice(),
                Math.max(ticketType.getCapacity() - allocated, 0));
    }

    /** Solo se enlazan URL https; evita esquemas como javascript: en un href. */
    static String safeHttpsUrl(String url) {
        if (url == null) {
            return null;
        }
        String clean = url.trim();
        return clean.regionMatches(true, 0, "https://", 0, "https://".length()) ? clean : null;
    }
}
