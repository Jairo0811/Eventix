package com.jairomatias.eventix.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.notification.entity.ReminderDeliveryStatus;
import com.jairomatias.eventix.notification.repository.EventReminderCandidate;
import com.jairomatias.eventix.notification.repository.EventReminderCandidateRepository;
import com.jairomatias.eventix.notification.repository.EventReminderDeliveryRepository;

@Component
@ConditionalOnExpression(
        "'${eventix.notifications.reminders.enabled:false}' == 'true' "
        + "and '${eventix.notifications.email.enabled:false}' == 'true'")
public class EventReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            EventReminderScheduler.class);
    private static final List<ReminderDeliveryStatus> DUE_STATUSES = List.of(
            ReminderDeliveryStatus.PENDING,
            ReminderDeliveryStatus.FAILED);

    private final EventReminderCandidateRepository candidateRepository;
    private final EventReminderDeliveryRepository deliveryRepository;
    private final EventReminderDeliveryProcessor deliveryProcessor;
    private final ReminderProperties properties;
    private final Clock clock;

    @Autowired
    public EventReminderScheduler(
            EventReminderCandidateRepository candidateRepository,
            EventReminderDeliveryRepository deliveryRepository,
            EventReminderDeliveryProcessor deliveryProcessor,
            ReminderProperties properties) {
        this(
                candidateRepository,
                deliveryRepository,
                deliveryProcessor,
                properties,
                Clock.systemDefaultZone());
    }

    EventReminderScheduler(
            EventReminderCandidateRepository candidateRepository,
            EventReminderDeliveryRepository deliveryRepository,
            EventReminderDeliveryProcessor deliveryProcessor,
            ReminderProperties properties,
            Clock clock) {
        this.candidateRepository = candidateRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryProcessor = deliveryProcessor;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${eventix.notifications.reminders.scan-interval:PT5M}")
    public void deliverUpcomingEventReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<EventReminderCandidate> candidates = candidateRepository
                .findCandidates(
                        now,
                        now.plus(properties.getAdvance()),
                        properties.getCandidateBatchSize());
        candidates.forEach(candidate ->
                candidateRepository.registerPending(candidate, now));

        List<Long> dueIds = deliveryRepository.findDueIds(
                DUE_STATUSES,
                ReminderDeliveryStatus.PENDING,
                now,
                PageRequest.of(0, properties.getDeliveryBatchSize()));
        dueIds.forEach(deliveryProcessor::process);
        if (!candidates.isEmpty() || !dueIds.isEmpty()) {
            LOGGER.info(
                    "Recordatorios: {} candidatos registrados y {} procesados.",
                    candidates.size(),
                    dueIds.size());
        }
    }
}
