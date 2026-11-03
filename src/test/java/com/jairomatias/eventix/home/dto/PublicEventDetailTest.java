package com.jairomatias.eventix.home.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PublicEventDetailTest {

    private static PublicEventDetail detail(String description, List<PublicTicketOption> tickets) {
        return new PublicEventDetail(
                1L, "Festival", description, "Música",
                LocalDateTime.of(2030, 1, 10, 20, 0),
                LocalDateTime.of(2030, 1, 10, 23, 0),
                "Teatro Nacional", "Av. Máximo Gómez", null, null,
                false, BigDecimal.TEN, true, tickets);
    }

    @Test
    void summaryCollapsesWhitespaceAndTruncatesLongDescriptions() {
        String longText = ("palabra ").repeat(60);

        String summary = detail(longText, List.of()).summary();

        assertThat(summary).hasSizeLessThanOrEqualTo(160).endsWith("…");
        assertThat(summary).doesNotContain("  ");
    }

    @Test
    void summaryFallsBackToTitleAndVenueWhenThereIsNoDescription() {
        assertThat(detail("  ", List.of()).summary()).contains("Festival").contains("Teatro Nacional");
    }

    @Test
    void soldOutOnlyWhenEveryTicketTypeIsSoldOut() {
        var gone = new PublicTicketOption("VIP", "VIP", BigDecimal.TEN, 0);
        var left = new PublicTicketOption("General", "General", BigDecimal.ONE, 5);

        assertThat(detail("x", List.of(gone)).soldOut()).isTrue();
        assertThat(detail("x", List.of(gone, left)).soldOut()).isFalse();
        assertThat(detail("x", List.of()).soldOut()).isFalse();
    }

    @Test
    void lowStockWarnsBelowThresholdButNotWhenSoldOut() {
        assertThat(new PublicTicketOption("a", "b", BigDecimal.ONE, 20).lowStock()).isTrue();
        assertThat(new PublicTicketOption("a", "b", BigDecimal.ONE, 21).lowStock()).isFalse();
        assertThat(new PublicTicketOption("a", "b", BigDecimal.ONE, 0).lowStock()).isFalse();
    }

    @Test
    void isoDatesIncludeTheDominicanOffset() {
        assertThat(detail("x", List.of()).startIso()).isEqualTo("2030-01-10T20:00:00-04:00");
    }
}
