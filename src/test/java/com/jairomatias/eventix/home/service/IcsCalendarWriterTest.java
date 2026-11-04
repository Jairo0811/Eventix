package com.jairomatias.eventix.home.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class IcsCalendarWriterTest {

    private static final ZoneId SANTO_DOMINGO = ZoneId.of("America/Santo_Domingo");
    private static final ZonedDateTime STAMP =
            ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void escapesTheCharactersReservedByTheIcsTextType() {
        String escaped = IcsCalendarWriter.escapeText("Rock; Pop, Jazz\\Blues");

        assertThat(escaped).isEqualTo("Rock\\; Pop\\, Jazz\\\\Blues");
    }

    @Test
    void escapesBackslashBeforeTheOtherCharacters() {
        // Si el orden fuera el inverso, la barra escapada se duplicaría.
        assertThat(IcsCalendarWriter.escapeText("a\\;b")).isEqualTo("a\\\\\\;b");
    }

    @Test
    void turnsEveryKindOfLineBreakIntoTheLiteralSequence() {
        assertThat(IcsCalendarWriter.escapeText("uno\r\ndos\rtres\ncuatro"))
                .isEqualTo("uno\\ndos\\ntres\\ncuatro");
    }

    @Test
    void nullTextBecomesEmpty() {
        assertThat(IcsCalendarWriter.escapeText(null)).isEmpty();
    }

    @Test
    void shortLinesAreNotFolded() {
        assertThat(IcsCalendarWriter.foldLine("SUMMARY:Concierto")).isEqualTo("SUMMARY:Concierto");
    }

    @Test
    void longLinesAreFoldedWithCrlfAndASpace() {
        String line = "SUMMARY:" + "a".repeat(200);

        String folded = IcsCalendarWriter.foldLine(line);

        assertThat(folded).contains("\r\n ");
        for (String segment : folded.split("\r\n")) {
            // Cada línea cabe en 75 octetos, incluido el espacio de continuación.
            assertThat(segment.getBytes(StandardCharsets.UTF_8).length)
                    .isLessThanOrEqualTo(75);
        }
    }

    @Test
    void foldingNeverSplitsAMultiByteCharacter() {
        String line = "SUMMARY:" + "ñ".repeat(120);

        String folded = IcsCalendarWriter.foldLine(line);

        // Si un carácter se partiera, al reconstruir aparecería el reemplazo U+FFFD.
        String unfolded = folded.replace("\r\n ", "");
        assertThat(unfolded).isEqualTo(line);
        assertThat(new String(folded.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .doesNotContain("�");
    }

    @Test
    void buildsAWholeCalendarWithUtcTimesAndCrlfLineEndings() {
        String ics = IcsCalendarWriter.forEvent(
                42L,
                "Concierto de Año Nuevo",
                "Teatro Nacional, Av. Máximo Gómez",
                "https://eventix.example.com/e/42",
                LocalDateTime.of(2026, 12, 31, 21, 0),
                LocalDateTime.of(2027, 1, 1, 1, 0),
                SANTO_DOMINGO,
                STAMP);

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("UID:event-42@eventix");
        assertThat(ics).contains("DTSTAMP:20261104T120000Z");
        // Santo Domingo es UTC-4 todo el año: 21:00 local es 01:00 UTC del día siguiente.
        assertThat(ics).contains("DTSTART:20270101T010000Z");
        assertThat(ics).contains("DTEND:20270101T050000Z");
        assertThat(ics).contains("LOCATION:Teatro Nacional\\, Av. Máximo Gómez");
        assertThat(ics).doesNotContain("\n\n");
    }

    @Test
    void omitsLocationAndUrlWhenThereIsNothingToShow() {
        String ics = IcsCalendarWriter.forEvent(
                7L, "Taller", "  ", null,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 12, 0),
                SANTO_DOMINGO, STAMP);

        assertThat(ics).doesNotContain("LOCATION:").doesNotContain("URL:");
    }
}
