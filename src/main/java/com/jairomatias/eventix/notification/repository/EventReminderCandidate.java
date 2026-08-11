package com.jairomatias.eventix.notification.repository;

public record EventReminderCandidate(
        Long eventId,
        String recipientEmail) {
}
