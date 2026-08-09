package com.jairomatias.eventix.reporting.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jairomatias.eventix.reporting.dto.CategoryReportRow;
import com.jairomatias.eventix.reporting.dto.EventReportRow;
import com.jairomatias.eventix.reporting.dto.MonthlyRevenueRow;
import com.jairomatias.eventix.reporting.dto.OrganizerReportRow;
import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportOption;
import com.jairomatias.eventix.reporting.dto.ReportSummary;

@Repository
public class ReportingRepository {

    private static final Locale SPANISH = Locale.of("es", "DO");
    private static final String EVENT_FILTER = """
            (:eventId IS NULL OR e.id = :eventId)
            AND (:categoryId IS NULL OR e.category_id = :categoryId)
            AND (:organizerId IS NULL OR e.organizer_id = :organizerId)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReportingRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ReportDataset generate(NormalizedReportFilter filter) {
        Map<Long, EventAccumulator> events = loadEvents(filter);
        applySales(events, filter);
        applyReservations(events, filter);
        applyAttendance(events, filter);

        List<EventReportRow> eventRows = events.values().stream()
                .map(EventAccumulator::toRow)
                .sorted(Comparator
                        .comparing(EventReportRow::revenue)
                        .reversed()
                        .thenComparing(EventReportRow::eventName))
                .toList();

        return new ReportDataset(
                filter.from(),
                filter.to(),
                summarize(eventRows),
                eventRows,
                aggregateCategories(eventRows),
                aggregateOrganizers(eventRows),
                loadMonthlyRevenue(filter));
    }

    public List<ReportOption> findEvents(Long organizerId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("organizerId", organizerId);
        return jdbcTemplate.query("""
                SELECT e.id, e.title
                FROM events e
                WHERE (:organizerId IS NULL OR e.organizer_id = :organizerId)
                ORDER BY e.start_at DESC, e.title ASC
                """, parameters, (rs, rowNum) -> new ReportOption(
                        rs.getLong("id"),
                        rs.getString("title")));
    }

    public List<ReportOption> findCategories() {
        return jdbcTemplate.query("""
                SELECT id, name
                FROM event_categories
                ORDER BY name ASC
                """, (rs, rowNum) -> new ReportOption(
                        rs.getLong("id"),
                        rs.getString("name")));
    }

    public List<ReportOption> findOrganizers() {
        return jdbcTemplate.query("""
                SELECT u.id,
                       CONCAT(u.first_name, N' ', u.last_name) AS full_name
                FROM users u
                INNER JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'ORGANIZER'
                ORDER BY u.last_name ASC, u.first_name ASC
                """, (rs, rowNum) -> new ReportOption(
                        rs.getLong("id"),
                        rs.getString("full_name")));
    }

    private Map<Long, EventAccumulator> loadEvents(
            NormalizedReportFilter filter) {
        Map<Long, EventAccumulator> events = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT e.id,
                       e.title,
                       c.id AS category_id,
                       c.name AS category_name,
                       o.id AS organizer_id,
                       CONCAT(o.first_name, N' ', o.last_name) AS organizer_name
                FROM events e
                INNER JOIN event_categories c ON c.id = e.category_id
                INNER JOIN users o ON o.id = e.organizer_id
                WHERE """ + EVENT_FILTER + """
                ORDER BY e.start_at DESC, e.title ASC
                """, parameters(filter), rs -> {
                    EventAccumulator row = new EventAccumulator(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getLong("category_id"),
                            rs.getString("category_name"),
                            rs.getLong("organizer_id"),
                            rs.getString("organizer_name"));
                    events.put(row.eventId, row);
                });
        return events;
    }

    private void applySales(
            Map<Long, EventAccumulator> events,
            NormalizedReportFilter filter) {
        jdbcTemplate.query("""
                SELECT e.id AS event_id,
                       COUNT(s.id) AS sales_count,
                       COALESCE(SUM(s.total), 0) AS revenue,
                       COALESCE(SUM(items.ticket_count), 0) AS tickets_sold
                FROM sales s
                INNER JOIN events e ON e.id = s.event_id
                LEFT JOIN (
                    SELECT sale_id, SUM(quantity) AS ticket_count
                    FROM sale_items
                    GROUP BY sale_id
                ) items ON items.sale_id = s.id
                WHERE s.status = 'PAID'
                  AND s.paid_at >= :fromDate
                  AND s.paid_at < :toDate
                  AND """ + EVENT_FILTER + """
                GROUP BY e.id
                """, parameters(filter), rs -> {
                    EventAccumulator row = events.get(rs.getLong("event_id"));
                    if (row != null) {
                        row.sales = rs.getLong("sales_count");
                        row.ticketsSold = rs.getLong("tickets_sold");
                        row.revenue = rs.getBigDecimal("revenue");
                    }
                });
    }

    private void applyReservations(
            Map<Long, EventAccumulator> events,
            NormalizedReportFilter filter) {
        jdbcTemplate.query("""
                SELECT e.id AS event_id,
                       COUNT(r.id) AS reservation_count,
                       COALESCE(SUM(r.quantity), 0) AS reserved_places
                FROM reservations r
                INNER JOIN events e ON e.id = r.event_id
                WHERE r.status IN ('PENDING', 'CONFIRMED')
                  AND r.created_at >= :fromDate
                  AND r.created_at < :toDate
                  AND """ + EVENT_FILTER + """
                GROUP BY e.id
                """, parameters(filter), rs -> {
                    EventAccumulator row = events.get(rs.getLong("event_id"));
                    if (row != null) {
                        row.reservations = rs.getLong("reservation_count");
                        row.reservedPlaces = rs.getLong("reserved_places");
                    }
                });
    }

    private void applyAttendance(
            Map<Long, EventAccumulator> events,
            NormalizedReportFilter filter) {
        jdbcTemplate.query("""
                SELECT e.id AS event_id,
                       COUNT(a.id) AS attendee_count
                FROM ticket_scan_attempts a
                INNER JOIN events e ON e.id = a.event_id
                WHERE a.first_access = 1
                  AND a.occurred_at >= :fromDate
                  AND a.occurred_at < :toDate
                  AND """ + EVENT_FILTER + """
                GROUP BY e.id
                """, parameters(filter), rs -> {
                    EventAccumulator row = events.get(rs.getLong("event_id"));
                    if (row != null) {
                        row.attendees = rs.getLong("attendee_count");
                    }
                });
    }

    private List<MonthlyRevenueRow> loadMonthlyRevenue(
            NormalizedReportFilter filter) {
        return jdbcTemplate.query("""
                SELECT YEAR(s.paid_at) AS report_year,
                       MONTH(s.paid_at) AS report_month,
                       COUNT(s.id) AS sales_count,
                       COALESCE(SUM(s.total), 0) AS revenue,
                       COALESCE(SUM(items.ticket_count), 0) AS tickets_sold
                FROM sales s
                INNER JOIN events e ON e.id = s.event_id
                LEFT JOIN (
                    SELECT sale_id, SUM(quantity) AS ticket_count
                    FROM sale_items
                    GROUP BY sale_id
                ) items ON items.sale_id = s.id
                WHERE s.status = 'PAID'
                  AND s.paid_at >= :fromDate
                  AND s.paid_at < :toDate
                  AND """ + EVENT_FILTER + """
                GROUP BY YEAR(s.paid_at), MONTH(s.paid_at)
                ORDER BY report_year ASC, report_month ASC
                """, parameters(filter), (rs, rowNum) -> {
                    int year = rs.getInt("report_year");
                    int month = rs.getInt("report_month");
                    String name = Month.of(month)
                            .getDisplayName(TextStyle.FULL, SPANISH);
                    String period = Character.toUpperCase(name.charAt(0))
                            + name.substring(1) + " " + year;
                    return new MonthlyRevenueRow(
                            year,
                            month,
                            period,
                            rs.getLong("sales_count"),
                            rs.getLong("tickets_sold"),
                            rs.getBigDecimal("revenue"));
                });
    }

    private ReportSummary summarize(List<EventReportRow> rows) {
        BigDecimal revenue = rows.stream()
                .map(EventReportRow::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long sales = rows.stream().mapToLong(EventReportRow::sales).sum();
        long tickets = rows.stream()
                .mapToLong(EventReportRow::ticketsSold).sum();
        long reservations = rows.stream()
                .mapToLong(EventReportRow::reservations).sum();
        long reservedPlaces = rows.stream()
                .mapToLong(EventReportRow::reservedPlaces).sum();
        long attendees = rows.stream()
                .mapToLong(EventReportRow::attendees).sum();
        return new ReportSummary(
                revenue,
                sales,
                tickets,
                reservations,
                reservedPlaces,
                attendees,
                percentage(sales, reservations),
                percentage(attendees, tickets));
    }

    private List<CategoryReportRow> aggregateCategories(
            List<EventReportRow> rows) {
        Map<Long, GroupAccumulator> groups = new LinkedHashMap<>();
        rows.forEach(row -> groups
                .computeIfAbsent(
                        row.categoryId(),
                        ignored -> new GroupAccumulator(row.categoryName()))
                .add(row));
        List<CategoryReportRow> result = new ArrayList<>();
        groups.forEach((id, value) -> result.add(new CategoryReportRow(
                id,
                value.name,
                value.events,
                value.sales,
                value.tickets,
                value.reservations,
                value.attendees,
                value.revenue)));
        return result.stream()
                .sorted(Comparator.comparing(CategoryReportRow::revenue)
                        .reversed())
                .toList();
    }

    private List<OrganizerReportRow> aggregateOrganizers(
            List<EventReportRow> rows) {
        Map<Long, GroupAccumulator> groups = new LinkedHashMap<>();
        rows.forEach(row -> groups
                .computeIfAbsent(
                        row.organizerId(),
                        ignored -> new GroupAccumulator(row.organizerName()))
                .add(row));
        List<OrganizerReportRow> result = new ArrayList<>();
        groups.forEach((id, value) -> result.add(new OrganizerReportRow(
                id,
                value.name,
                value.events,
                value.sales,
                value.tickets,
                value.reservations,
                value.attendees,
                value.revenue)));
        return result.stream()
                .sorted(Comparator.comparing(OrganizerReportRow::revenue)
                        .reversed())
                .toList();
    }

    private MapSqlParameterSource parameters(NormalizedReportFilter filter) {
        return new MapSqlParameterSource()
                .addValue("fromDate", Timestamp.valueOf(filter.fromInclusive()))
                .addValue("toDate", Timestamp.valueOf(filter.toExclusive()))
                .addValue("eventId", filter.eventId())
                .addValue("categoryId", filter.categoryId())
                .addValue("organizerId", filter.organizerId());
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2,
                        java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    private static final class EventAccumulator {
        private final Long eventId;
        private final String eventName;
        private final Long categoryId;
        private final String categoryName;
        private final Long organizerId;
        private final String organizerName;
        private long sales;
        private long ticketsSold;
        private long reservations;
        private long reservedPlaces;
        private long attendees;
        private BigDecimal revenue = BigDecimal.ZERO;

        private EventAccumulator(
                Long eventId,
                String eventName,
                Long categoryId,
                String categoryName,
                Long organizerId,
                String organizerName) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.organizerId = organizerId;
            this.organizerName = organizerName;
        }

        private EventReportRow toRow() {
            return new EventReportRow(
                    eventId,
                    eventName,
                    categoryId,
                    categoryName,
                    organizerId,
                    organizerName,
                    sales,
                    ticketsSold,
                    reservations,
                    reservedPlaces,
                    attendees,
                    revenue);
        }
    }

    private static final class GroupAccumulator {
        private final String name;
        private long events;
        private long sales;
        private long tickets;
        private long reservations;
        private long attendees;
        private BigDecimal revenue = BigDecimal.ZERO;

        private GroupAccumulator(String name) {
            this.name = name;
        }

        private void add(EventReportRow row) {
            events++;
            sales += row.sales();
            tickets += row.ticketsSold();
            reservations += row.reservations();
            attendees += row.attendees();
            revenue = revenue.add(row.revenue());
        }
    }
}
