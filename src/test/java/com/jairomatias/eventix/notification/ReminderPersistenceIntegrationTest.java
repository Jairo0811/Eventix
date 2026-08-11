package com.jairomatias.eventix.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.notification.repository.EventReminderCandidate;
import com.jairomatias.eventix.notification.repository.EventReminderCandidateRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReminderPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventReminderCandidateRepository candidateRepository;

    @Test
    void registrationUsesDatabaseUniquenessToDeduplicateReminder() {
        Long eventId = jdbcTemplate.queryForObject("""
                SET NOCOUNT ON;
                INSERT INTO events (
                    title, description, category_id, status,
                    start_at, end_at, venue, address, capacity,
                    organizer_id, is_free, base_price)
                VALUES (
                    N'Evento recordatorio', N'Prueba de deduplicación', 1,
                    'PUBLISHED', '2026-08-11T12:00:00',
                    '2026-08-11T14:00:00', N'Eventix', N'Santo Domingo',
                    100, 1, 1, 0);
                SELECT CAST(SCOPE_IDENTITY() AS BIGINT);
                """, Long.class);
        EventReminderCandidate candidate = new EventReminderCandidate(
                eventId,
                "buyer@example.com");
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 12, 0);

        candidateRepository.registerPending(candidate, now);
        candidateRepository.registerPending(candidate, now);

        Integer deliveries = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM event_reminder_deliveries
                WHERE event_id = ? AND recipient_email = ?
                """, Integer.class, eventId, "buyer@example.com");
        assertThat(deliveries).isEqualTo(1);
    }
}
