package com.jairomatias.eventix.home.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Construye archivos iCalendar (RFC 5545) para los eventos del catálogo
 * público. Se mantiene aparte del controlador porque el escapado y el plegado
 * de líneas son reglas del formato, no del transporte HTTP, y así pueden
 * probarse sin levantar el contexto de Spring.
 */
public final class IcsCalendarWriter {

    private static final DateTimeFormatter UTC_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /** RFC 5545, 3.1: una línea no debe exceder 75 octetos sin plegarse. */
    private static final int MAX_OCTETS = 75;

    private IcsCalendarWriter() {
    }

    /** Devuelve el contenido completo de un .ics de un solo evento. */
    public static String forEvent(
            long eventId,
            String title,
            String location,
            String url,
            LocalDateTime startAt,
            LocalDateTime endAt,
            ZoneId zone,
            ZonedDateTime stamp) {
        List<String> lines = new ArrayList<>();
        lines.add("BEGIN:VCALENDAR");
        lines.add("VERSION:2.0");
        lines.add("PRODID:-//Eventix//Eventos//ES");
        lines.add("CALSCALE:GREGORIAN");
        lines.add("BEGIN:VEVENT");
        lines.add("UID:event-" + eventId + "@eventix");
        lines.add("DTSTAMP:" + UTC_STAMP.format(stamp.withZoneSameInstant(ZoneOffset.UTC)));
        lines.add("DTSTART:" + utc(startAt, zone));
        lines.add("DTEND:" + utc(endAt, zone));
        lines.add("SUMMARY:" + escapeText(title));
        if (location != null && !location.isBlank()) {
            lines.add("LOCATION:" + escapeText(location));
        }
        if (url != null && !url.isBlank()) {
            lines.add("URL:" + escapeText(url));
        }
        lines.add("END:VEVENT");
        lines.add("END:VCALENDAR");

        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(foldLine(line)).append("\r\n");
        }
        return out.toString();
    }

    static String utc(LocalDateTime value, ZoneId zone) {
        return UTC_STAMP.format(value.atZone(zone).withZoneSameInstant(ZoneOffset.UTC));
    }

    /**
     * Escapa un valor TEXT según RFC 5545, 3.3.11. La barra invertida debe
     * sustituirse primero para no duplicar las que introducen los demás casos.
     */
    static String escapeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\r", "\\n")
                .replace("\n", "\\n");
    }

    /**
     * Pliega la línea en trozos de 75 octetos como máximo, sin partir un
     * carácter multibyte: importante porque los títulos en español llevan
     * acentos y la ñ, que ocupan dos octetos en UTF-8.
     */
    static String foldLine(String line) {
        if (utf8Length(line) <= MAX_OCTETS) {
            return line;
        }
        StringBuilder folded = new StringBuilder();
        int used = 0;
        // Las continuaciones empiezan por un espacio que cuenta para el límite.
        int limit = MAX_OCTETS;
        for (int index = 0; index < line.length();) {
            int codePoint = line.codePointAt(index);
            int width = Character.charCount(codePoint);
            int octets = utf8Length(new String(Character.toChars(codePoint)));
            if (used + octets > limit) {
                folded.append("\r\n ");
                used = 1;
                limit = MAX_OCTETS;
            }
            folded.appendCodePoint(codePoint);
            used += octets;
            index += width;
        }
        return folded.toString();
    }

    private static int utf8Length(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
}
