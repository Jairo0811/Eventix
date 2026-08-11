package com.jairomatias.eventix.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.notification.entity.EventReminderDelivery;
import com.jairomatias.eventix.notification.repository.EventReminderDeliveryRepository;

class EventReminderDeliveryProcessorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 8, 10, 12, 0);

    @Test
    void providerFailureIsRecordedWithoutEscapingWorkerTransaction() {
        EventReminderDeliveryRepository repository = mock(
                EventReminderDeliveryRepository.class);
        NotificationPreferenceService preferenceService = mock(
                NotificationPreferenceService.class);
        NotificationService notificationService = mock(
                NotificationService.class);
        ReminderProperties properties = new ReminderProperties();
        properties.setRetryDelay(Duration.ofMinutes(10));
        properties.setMaximumAttempts(3);
        EventReminderDelivery delivery = mock(EventReminderDelivery.class);
        Event event = mock(Event.class);
        when(repository.findDetailedByIdForUpdate(7L))
                .thenReturn(Optional.of(delivery));
        when(delivery.isDue(NOW)).thenReturn(true);
        when(delivery.getRecipientEmail()).thenReturn("buyer@example.com");
        when(delivery.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(9L);
        when(event.getTitle()).thenReturn("Eventix Live");
        when(preferenceService.allowsEventReminders("buyer@example.com"))
                .thenReturn(true);
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(notificationService)
                .sendEventReminder("buyer@example.com", "Eventix Live");
        EventReminderDeliveryProcessor processor = processor(
                repository,
                preferenceService,
                notificationService,
                properties);

        assertThatCode(() -> processor.process(7L)).doesNotThrowAnyException();

        verify(delivery).markFailed(
                NOW,
                Duration.ofMinutes(10),
                3,
                "IllegalStateException");
    }

    @Test
    void disabledPreferenceSkipsDeliveryWithoutCallingProvider() {
        EventReminderDeliveryRepository repository = mock(
                EventReminderDeliveryRepository.class);
        NotificationPreferenceService preferenceService = mock(
                NotificationPreferenceService.class);
        NotificationService notificationService = mock(
                NotificationService.class);
        ReminderProperties properties = new ReminderProperties();
        EventReminderDelivery delivery = mock(EventReminderDelivery.class);
        when(repository.findDetailedByIdForUpdate(8L))
                .thenReturn(Optional.of(delivery));
        when(delivery.isDue(NOW)).thenReturn(true);
        when(delivery.getRecipientEmail()).thenReturn("buyer@example.com");
        when(preferenceService.allowsEventReminders("buyer@example.com"))
                .thenReturn(false);
        EventReminderDeliveryProcessor processor = processor(
                repository,
                preferenceService,
                notificationService,
                properties);

        processor.process(8L);

        verify(delivery).markSkipped(NOW);
        org.mockito.Mockito.verifyNoInteractions(notificationService);
    }

    private EventReminderDeliveryProcessor processor(
            EventReminderDeliveryRepository repository,
            NotificationPreferenceService preferenceService,
            NotificationService notificationService,
            ReminderProperties properties) {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T12:00:00Z"),
                ZoneOffset.UTC);
        return new EventReminderDeliveryProcessor(
                repository,
                preferenceService,
                notificationService,
                properties,
                clock);
    }
}
