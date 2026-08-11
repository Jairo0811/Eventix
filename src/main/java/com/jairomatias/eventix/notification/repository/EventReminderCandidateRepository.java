package com.jairomatias.eventix.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventReminderCandidateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EventReminderCandidateRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<EventReminderCandidate> findCandidates(
            LocalDateTime now,
            LocalDateTime reminderLimit,
            int batchSize) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("now", now)
                .addValue("reminderLimit", reminderLimit)
                .addValue("batchSize", batchSize);
        return jdbcTemplate.query("""
                SELECT TOP (:batchSize)
                       t.event_id,
                       LOWER(t.attendee_email) AS recipient_email
                FROM digital_tickets t
                INNER JOIN events e ON e.id = t.event_id
                WHERE t.status = 'ACTIVE'
                  AND e.status = 'PUBLISHED'
                  AND e.start_at > :now
                  AND e.start_at <= :reminderLimit
                  AND NOT EXISTS (
                      SELECT 1
                      FROM event_reminder_deliveries d
                      WHERE d.event_id = t.event_id
                        AND d.recipient_email = LOWER(t.attendee_email)
                  )
                GROUP BY t.event_id, LOWER(t.attendee_email), e.start_at
                ORDER BY e.start_at ASC, t.event_id ASC
                """, parameters, (rs, rowNum) -> new EventReminderCandidate(
                rs.getLong("event_id"),
                rs.getString("recipient_email")));
    }

    @Transactional
    public void registerPending(
            EventReminderCandidate candidate,
            LocalDateTime now) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", candidate.eventId())
                .addValue("recipientEmail", candidate.recipientEmail())
                .addValue("now", now);
        jdbcTemplate.update("""
                MERGE event_reminder_deliveries WITH (HOLDLOCK) AS target
                USING (SELECT :eventId AS event_id,
                              :recipientEmail AS recipient_email) AS source
                ON target.event_id = source.event_id
                   AND target.recipient_email = source.recipient_email
                WHEN NOT MATCHED THEN
                    INSERT (
                        event_id,
                        recipient_email,
                        status,
                        attempt_count,
                        created_at,
                        updated_at,
                        created_by,
                        updated_by,
                        version)
                    VALUES (
                        source.event_id,
                        source.recipient_email,
                        'PENDING',
                        0,
                        :now,
                        :now,
                        'reminder-scheduler',
                        'reminder-scheduler',
                        0);
                """, parameters);
    }
}
