package com.jairomatias.eventix.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EventReminderDeliveryTest {

    @Test
    void retriesFailureUntilMaximumAttemptsWithoutDuplicatingDelivery() {
        EventReminderDelivery delivery = new EventReminderDelivery();
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 12, 0);

        delivery.markFailed(
                now,
                Duration.ofMinutes(15),
                2,
                "MailSendException");

        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt())
                .isEqualTo(now.plusMinutes(15));
        assertThat(delivery.isDue(now.plusMinutes(14))).isFalse();
        assertThat(delivery.isDue(now.plusMinutes(15))).isTrue();

        delivery.markFailed(
                now.plusMinutes(15),
                Duration.ofMinutes(15),
                2,
                "MailSendException");

        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(delivery.getNextAttemptAt()).isNull();
        assertThat(delivery.isDue(now.plusHours(1))).isFalse();
    }

    @Test
    void sentDeliveryCannotBecomeDueAgain() {
        EventReminderDelivery delivery = new EventReminderDelivery();
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 12, 0);

        delivery.markSent(now);

        assertThat(delivery.getStatus())
                .isEqualTo(ReminderDeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isEqualTo(now);
        assertThat(delivery.isDue(now.plusDays(1))).isFalse();
    }
}
