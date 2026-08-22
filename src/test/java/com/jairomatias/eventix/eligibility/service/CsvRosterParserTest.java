package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvRosterParserTest {

    private final CsvRosterParser parser = new CsvRosterParser();

    @Test
    void parsesQuotedNamesAndOptionalFields() {
        String csv = """
                full_name,student_code,national_id,source_reference
                "Pérez, Ana María",A-2017-01,001-1234567-8,"Acta 2017, folio 4"
                Juan Rodríguez,,40212345678,
                """;

        var rows = parser.parse(csv.getBytes(StandardCharsets.UTF_8));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).fullName()).isEqualTo("Pérez, Ana María");
        assertThat(rows.get(0).sourceReference()).isEqualTo("Acta 2017, folio 4");
        assertThat(rows.get(1).studentCode()).isNull();
        assertThat(rows.get(1).sourceReference()).isNull();
    }

    @Test
    void rejectsUnexpectedHeader() {
        String csv = "name,id\nAna,00112345678\n";

        assertThatThrownBy(() -> parser.parse(csv.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Encabezado inválido");
    }
}
