package com.jairomatias.eventix.reporting.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NormalizedReportFilter(
        LocalDate from,
        LocalDate to,
        LocalDateTime fromInclusive,
        LocalDateTime toExclusive,
        Long eventId,
        Long categoryId,
        Long organizerId) {
}
