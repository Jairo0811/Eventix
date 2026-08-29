package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvRosterParserTest {

    private final CsvRosterParser parser = new CsvRosterParser();

    @Test
    void parsesQuotedNamesAndOptionalFieldsWithoutNationalId() {
        String csv = """
                full_name,student_code,source_reference
                "Pérez, Ana María",A-2017-01,"Acta 2017, folio 4"
                Juan Rodríguez,,
                """;

        var rows = parser.parse(csv.getBytes(StandardCharsets.UTF_8));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).fullName()).isEqualTo("Pérez, Ana María");
        assertThat(rows.get(0).sourceReference()).isEqualTo("Acta 2017, folio 4");
        assertThat(rows.get(1).studentCode()).isNull();
        assertThat(rows.get(1).sourceReference()).isNull();
    }

    @Test
    void acceptsMinimalRosterContainingOnlyNames() {
        String csv = "full_name\nAna Perez Gomez\nLuis Ramirez Santos\n";

        var rows = parser.parse(csv.getBytes(StandardCharsets.UTF_8));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).studentCode()).isNull();
        assertThat(rows.get(0).sourceReference()).isNull();
    }

    @Test
    void rejectsNationalIdColumnBecauseItDoesNotBelongToRoster() {
        String csv = "full_name,national_id\nAna,00112345678\n";

        assertThatThrownBy(() -> parser.parse(csv.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Encabezado inválido");
    }
}
