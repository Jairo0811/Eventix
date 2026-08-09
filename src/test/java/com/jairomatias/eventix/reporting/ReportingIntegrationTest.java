package com.jairomatias.eventix.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.reporting.dto.ReportDataset;
import com.jairomatias.eventix.reporting.dto.ReportFilter;
import com.jairomatias.eventix.reporting.service.ReportService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportingIntegrationTest {

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 8, 8, 10, 0);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportService reportService;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void aggregatesSalesReservationsAndAttendanceOnSqlServer() {
        long administratorId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'admin'",
                Long.class);
        long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM event_categories WHERE name = N'Conferencia'",
                Long.class);
        long eventId = insert("events", map(
                "title", "Evento de integración de reportes",
                "description", "Datos verificables de la Fase 6.",
                "category_id", categoryId,
                "status", "PUBLISHED",
                "start_at", LocalDateTime.of(2026, 8, 20, 18, 0),
                "end_at", LocalDateTime.of(2026, 8, 20, 21, 0),
                "venue", "Centro Eventix",
                "address", "Santo Domingo",
                "capacity", 100,
                "organizer_id", administratorId,
                "is_free", false,
                "base_price", new BigDecimal("125.00"),
                "created_at", CREATED_AT,
                "updated_at", CREATED_AT,
                "created_by", "integration-test",
                "updated_by", "integration-test",
                "version", 0L));
        long reservationId = insert("reservations", map(
                "reference_code", "RES-REPORT-0001",
                "event_id", eventId,
                "attendee_first_name", "Ana",
                "attendee_last_name", "Prueba",
                "attendee_email", "ana.report@example.com",
                "attendee_phone", "8095550101",
                "quantity", 2,
                "status", "CONFIRMED",
                "expires_at", CREATED_AT.plusMinutes(15),
                "confirmed_at", CREATED_AT.plusMinutes(1),
                "reserved_by_id", administratorId,
                "created_at", CREATED_AT,
                "updated_at", CREATED_AT,
                "created_by", "integration-test",
                "updated_by", "integration-test",
                "version", 0L));
        long ticketTypeId = insert("ticket_types", map(
                "event_id", eventId,
                "category", "GENERAL",
                "name", "General integración",
                "price", new BigDecimal("125.00"),
                "capacity", 100,
                "active", true,
                "created_at", CREATED_AT,
                "updated_at", CREATED_AT,
                "created_by", "integration-test",
                "updated_by", "integration-test",
                "version", 0L));
        long saleId = insert("sales", map(
                "reference_code", "SAL-REPORT-0001",
                "reservation_id", reservationId,
                "event_id", eventId,
                "buyer_name", "Ana Prueba",
                "buyer_email", "ana.report@example.com",
                "buyer_phone", "8095550101",
                "status", "PAID",
                "currency", "DOP",
                "subtotal", new BigDecimal("250.00"),
                "discount_total", BigDecimal.ZERO,
                "total", new BigDecimal("250.00"),
                "paid_at", CREATED_AT.plusMinutes(2),
                "sold_by_id", administratorId,
                "created_at", CREATED_AT,
                "updated_at", CREATED_AT,
                "created_by", "integration-test",
                "updated_by", "integration-test",
                "version", 0L));
        insert("sale_items", map(
                "sale_id", saleId,
                "ticket_type_id", ticketTypeId,
                "ticket_type_name", "General integración",
                "quantity", 2,
                "unit_price", new BigDecimal("125.00"),
                "subtotal", new BigDecimal("250.00")));
        insert("ticket_scan_attempts", map(
                "event_id", eventId,
                "raw_code_hash", "a".repeat(64),
                "outcome", "VALID",
                "occurred_at", CREATED_AT.plusDays(12),
                "scanned_by_id", administratorId,
                "device_identifier", "integration-scanner",
                "ip_address", "192.0.2.20",
                "first_access", true,
                "duplicate_attempt", false,
                "notes", "Prueba de reporte"));

        ReportFilter filter = new ReportFilter();
        filter.setFrom(LocalDate.of(2026, 8, 1));
        filter.setTo(LocalDate.of(2026, 8, 31));
        filter.setEventId(eventId);
        ReportDataset report = reportService.generate(filter, null);

        assertThat(report.summary().revenue())
                .isEqualByComparingTo("250.00");
        assertThat(report.summary().sales()).isEqualTo(1);
        assertThat(report.summary().ticketsSold()).isEqualTo(2);
        assertThat(report.summary().reservations()).isEqualTo(1);
        assertThat(report.summary().attendees()).isEqualTo(1);
        assertThat(report.byEvent()).singleElement().satisfies(row -> {
            assertThat(row.eventName())
                    .isEqualTo("Evento de integración de reportes");
            assertThat(row.revenue()).isEqualByComparingTo("250.00");
        });
    }

    private long insert(String table, Map<String, Object> values) {
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(table)
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(values)
                .longValue();
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new HashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index].toString(), values[index + 1]);
        }
        return result;
    }
}
