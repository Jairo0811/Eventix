package com.jairomatias.eventix.ticket.dto;

public record AccessDashboardSummary(
        long capacity,
        long issued,
        long attendees,
        long pending,
        long rejected,
        long duplicateAttempts,
        long reentries) {
}
