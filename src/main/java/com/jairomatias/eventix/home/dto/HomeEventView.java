package com.jairomatias.eventix.home.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record HomeEventView(
        Long id,
        String title,
        String categoryName,
        LocalDateTime startAt,
        String venue,
        String coverImageUrl,
        boolean freeEvent,
        BigDecimal basePrice) {

    private static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-DO");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE d MMM", SPANISH_LOCALE);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a", SPANISH_LOCALE);

    public String dateLabel() {
        return DATE_FORMATTER.format(startAt);
    }

    public String timeLabel() {
        return TIME_FORMATTER.format(startAt);
    }
}
