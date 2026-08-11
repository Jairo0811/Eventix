package com.jairomatias.eventix.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.notification.entity.EventReminderDelivery;
import com.jairomatias.eventix.notification.repository.EventReminderDeliveryRepository;

@Service
public class EventReminderDeliveryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            EventReminderDeliveryProcessor.class);

    private final EventReminderDeliveryRepository deliveryRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationService notificationService;
    private final ReminderProperties properties;
    private final Clock clock;

    @Autowired
    public EventReminderDeliveryProcessor(
            EventReminderDeliveryRepository deliveryRepository,
            NotificationPreferenceService preferenceService,
            NotificationService notificationService,
            ReminderProperties properties) {
        this(
                deliveryRepository,
                preferenceService,
                notificationService,
                properties,
                Clock.systemDefaultZone());
    }

    EventReminderDeliveryProcessor(
            EventReminderDeliveryRepository deliveryRepository,
            NotificationPreferenceService preferenceService,
            NotificationService notificationService,
            ReminderProperties properties,
            Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.preferenceService = preferenceService;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long deliveryId) {
        LocalDateTime now = LocalDateTime.now(clock);
        EventReminderDelivery delivery = deliveryRepository
                .findDetailedByIdForUpdate(deliveryId)
                .orElse(null);
        if (delivery == null || !delivery.isDue(now)) {
            return;
        }
        MDC.put("correlationId", "event-reminder-" + deliveryId);
        try {
            if (!preferenceService.allowsEventReminders(
                    delivery.getRecipientEmail())) {
                delivery.markSkipped(now);
                return;
            }
            notificationService.sendEventReminder(
                    delivery.getRecipientEmail(),
                    delivery.getEvent().getTitle());
            delivery.markSent(now);
        } catch (RuntimeException exception) {
            delivery.markFailed(
                    now,
                    properties.getRetryDelay(),
                    properties.getMaximumAttempts(),
                    exception.getClass().getSimpleName());
            LOGGER.warn(
                    "No se pudo entregar el recordatorio {} del evento {}.",
                    deliveryId,
                    delivery.getEvent().getId(),
                    exception);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
