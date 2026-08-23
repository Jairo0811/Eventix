package com.jairomatias.eventix.dashboard.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jairomatias.eventix.dashboard.dto.OrganizerDashboardMetrics;
import com.jairomatias.eventix.dashboard.dto.OrganizerUpcomingEvent;

@Repository
public class OrganizerDashboardRepository {

    private static final int RATE_SCALE = 2;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OrganizerDashboardRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OrganizerDashboardMetrics loadMetrics(
            Long organizerId,
            LocalDateTime now) {
        MapSqlParameterSource parameters = parameters(organizerId, now);
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM events e
                     WHERE e.organizer_id = :organizerId
                       AND e.status = 'PUBLISHED'
                       AND e.start_at > :now) AS upcoming_events,
                    (SELECT COUNT(*) FROM events e
                     WHERE e.organizer_id = :organizerId
                       AND e.status = 'PUBLISHED') AS published_events,
                    (SELECT COALESCE(SUM(e.capacity), 0) FROM events e
                     WHERE e.organizer_id = :organizerId
                       AND e.status = 'PUBLISHED') AS published_capacity,
                    (SELECT COUNT(*)
                     FROM digital_tickets dt
                     INNER JOIN events e ON e.id = dt.event_id
                     WHERE e.organizer_id = :organizerId
                       AND dt.status IN ('ACTIVE', 'USED')) AS tickets_sold,
                    (SELECT COUNT(*) FROM reservations r
                     INNER JOIN events e ON e.id = r.event_id
                     WHERE e.organizer_id = :organizerId
                       AND (r.status = 'CONFIRMED'
                            OR (r.status = 'PENDING' AND r.expires_at > :now)))
                        AS active_reservations,
                    (SELECT COALESCE(SUM(s.subtotal), 0) FROM sales s
                     INNER JOIN events e ON e.id = s.event_id
                     WHERE e.organizer_id = :organizerId
                       AND s.status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')) AS gross_sales,
                    (SELECT COALESCE(SUM(s.discount_total), 0) FROM sales s
                     INNER JOIN events e ON e.id = s.event_id
                     WHERE e.organizer_id = :organizerId
                       AND s.status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')) AS discounts,
                    (SELECT COALESCE(SUM(s.refunded_amount), 0) FROM sales s
                     INNER JOIN events e ON e.id = s.event_id
                     WHERE e.organizer_id = :organizerId
                       AND s.status IN ('PARTIALLY_REFUNDED', 'REFUNDED')) AS refunds,
                    (SELECT COALESCE(SUM(s.platform_fee_amount), 0) FROM sales s
                     INNER JOIN events e ON e.id = s.event_id
                     WHERE e.organizer_id = :organizerId
                       AND s.status IN ('PAID', 'PARTIALLY_REFUNDED')) AS platform_commission,
                    (SELECT COALESCE(SUM(s.organizer_net_amount), 0) FROM sales s
                     INNER JOIN events e ON e.id = s.event_id
                     WHERE e.organizer_id = :organizerId
                       AND s.status IN ('PAID', 'PARTIALLY_REFUNDED')) AS estimated_net,
                    (SELECT COUNT(*) FROM organizer_settlements st
                     WHERE st.organizer_id = :organizerId
                       AND st.status IN ('PENDING', 'PROCESSING'))
                        AS pending_settlements,
                    (SELECT COUNT(*) FROM organizer_settlements st
                     WHERE st.organizer_id = :organizerId
                       AND st.status = 'PAID') AS paid_settlements,
                    (SELECT COALESCE(SUM(st.organizer_net), 0)
                     FROM organizer_settlements st
                     WHERE st.organizer_id = :organizerId
                       AND st.status IN ('PENDING', 'PROCESSING'))
                        AS pending_settlement_net,
                    (SELECT COALESCE(SUM(st.organizer_net), 0)
                     FROM organizer_settlements st
                     WHERE st.organizer_id = :organizerId
                       AND st.status = 'PAID') AS paid_settlement_net
                """, parameters, (rs, rowNum) -> new OrganizerDashboardMetrics(
                rs.getLong("upcoming_events"),
                rs.getLong("published_events"),
                rs.getLong("published_capacity"),
                rs.getLong("tickets_sold"),
                rs.getLong("active_reservations"),
                rs.getBigDecimal("gross_sales"),
                rs.getBigDecimal("discounts"),
                rs.getBigDecimal("refunds"),
                rs.getBigDecimal("platform_commission"),
                rs.getBigDecimal("estimated_net"),
                rs.getLong("pending_settlements"),
                rs.getLong("paid_settlements"),
                rs.getBigDecimal("pending_settlement_net"),
                rs.getBigDecimal("paid_settlement_net")));
    }

    public List<OrganizerUpcomingEvent> loadUpcomingEvents(
            Long organizerId,
            LocalDateTime now) {
        return jdbcTemplate.query("""
                SELECT TOP (5)
                       e.id,
                       e.title,
                       e.start_at,
                       e.capacity,
                       COALESCE(sales.tickets_sold, 0) AS tickets_sold,
                       COALESCE(reservations.active_count, 0)
                           AS active_reservations,
                       COALESCE(sales.paid_revenue, 0) AS paid_revenue,
                       COALESCE(sales.estimated_net, 0) AS estimated_net
                FROM events e
                OUTER APPLY (
                    SELECT
                        (SELECT COUNT(*) FROM digital_tickets dt
                         WHERE dt.event_id = e.id
                           AND dt.status IN ('ACTIVE', 'USED')) AS tickets_sold,
                        SUM(CASE WHEN s.status IN ('PAID', 'PARTIALLY_REFUNDED')
                                 THEN s.total - s.refunded_amount ELSE 0 END) AS paid_revenue,
                        SUM(CASE WHEN s.status IN ('PAID', 'PARTIALLY_REFUNDED')
                                 THEN s.organizer_net_amount ELSE 0 END) AS estimated_net
                    FROM sales s
                    WHERE s.event_id = e.id
                ) sales
                OUTER APPLY (
                    SELECT COUNT(*) AS active_count
                    FROM reservations r
                    WHERE r.event_id = e.id
                      AND (r.status = 'CONFIRMED'
                           OR (r.status = 'PENDING' AND r.expires_at > :now))
                ) reservations
                WHERE e.organizer_id = :organizerId
                  AND e.status = 'PUBLISHED'
                  AND e.start_at > :now
                ORDER BY e.start_at ASC
                """, parameters(organizerId, now), (rs, rowNum) -> {
                    int capacity = rs.getInt("capacity");
                    long tickets = rs.getLong("tickets_sold");
                    return new OrganizerUpcomingEvent(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getTimestamp("start_at").toLocalDateTime(),
                            capacity,
                            tickets,
                            rs.getLong("active_reservations"),
                            percentage(tickets, capacity),
                            rs.getBigDecimal("paid_revenue"),
                            rs.getBigDecimal("estimated_net"));
                });
    }

    private MapSqlParameterSource parameters(
            Long organizerId,
            LocalDateTime now) {
        return new MapSqlParameterSource()
                .addValue("organizerId", organizerId)
                .addValue("now", now);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(new BigDecimal("100"))
                .divide(
                        BigDecimal.valueOf(denominator),
                        RATE_SCALE,
                        RoundingMode.HALF_UP);
    }
}
