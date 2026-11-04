package com.jairomatias.eventix.home.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Datos de un evento publicado, listos para la página pública. */
public record PublicEventDetail(
        Long id,
        String title,
        String description,
        String categoryName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String venue,
        String address,
        String googleMapsUrl,
        String coverImageUrl,
        boolean freeEvent,
        BigDecimal basePrice,
        boolean onSale,
        List<PublicTicketOption> tickets) {

    /** Zona horaria en la que se capturan las fechas de los eventos. */
    public static final ZoneId EVENT_ZONE = ZoneId.of("America/Santo_Domingo");

    private static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-DO");
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", SPANISH_LOCALE);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", SPANISH_LOCALE);
    private static final int META_DESCRIPTION_LIMIT = 160;

    public String longDateLabel() {
        return LONG_DATE.format(startAt);
    }

    public String timeLabel() {
        return TIME.format(startAt);
    }

    public String endTimeLabel() {
        return TIME.format(endAt);
    }

    /** Fecha de inicio en ISO-8601 con desplazamiento (para JSON-LD). */
    public String startIso() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(startAt.atZone(EVENT_ZONE));
    }

    public String endIso() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(endAt.atZone(EVENT_ZONE));
    }

    public boolean finished() {
        return !startAt.isAfter(LocalDateTime.now(EVENT_ZONE));
    }

    public boolean soldOut() {
        return !tickets.isEmpty() && tickets.stream().allMatch(PublicTicketOption::soldOut);
    }

    /** Resumen corto para meta description y tarjetas al compartir. */
    public String summary() {
        String text = description == null ? "" : description.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return title + " en " + venue + ". Compra tus entradas en Eventix.";
        }
        return text.length() <= META_DESCRIPTION_LIMIT
                ? text
                : text.substring(0, META_DESCRIPTION_LIMIT - 1).trim() + "…";
    }
}
