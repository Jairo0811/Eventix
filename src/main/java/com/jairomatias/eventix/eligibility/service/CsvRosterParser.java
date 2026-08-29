package com.jairomatias.eventix.eligibility.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.jairomatias.eventix.eligibility.dto.RosterImportRow;

@Component
public class CsvRosterParser {

    private static final String FULL_NAME = "full_name";
    private static final String STUDENT_CODE = "student_code";
    private static final String SOURCE_REFERENCE = "source_reference";
    private static final Set<String> ALLOWED_HEADERS = Set.of(
            FULL_NAME,
            STUDENT_CODE,
            SOURCE_REFERENCE);

    public List<RosterImportRow> parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo CSV está vacío.");
        }

        String text = new String(content, StandardCharsets.UTF_8)
                .replace("\uFEFF", "");
        List<List<String>> rows = parseCsv(text);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV no contiene filas.");
        }

        List<String> header = rows.getFirst().stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
        validateHeader(header);

        int fullNameIndex = header.indexOf(FULL_NAME);
        int studentCodeIndex = header.indexOf(STUDENT_CODE);
        int sourceReferenceIndex = header.indexOf(SOURCE_REFERENCE);

        List<RosterImportRow> result = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (row.size() != header.size()) {
                throw new IllegalArgumentException(
                        "La fila " + (index + 1) + " no contiene la misma cantidad de columnas que el encabezado.");
            }
            result.add(new RosterImportRow(
                    index + 1,
                    row.get(fullNameIndex).trim(),
                    optionalValue(row, studentCodeIndex),
                    optionalValue(row, sourceReferenceIndex)));
        }
        return result;
    }

    private void validateHeader(List<String> header) {
        if (header.isEmpty()
                || !header.contains(FULL_NAME)
                || header.stream().anyMatch(value -> !ALLOWED_HEADERS.contains(value))
                || new HashSet<>(header).size() != header.size()) {
            throw new IllegalArgumentException(
                    "Encabezado inválido. 'full_name' es obligatorio; 'student_code' y 'source_reference' son opcionales.");
        }
    }

    private String optionalValue(List<String> row, int index) {
        return index < 0 ? null : nullable(row.get(index));
    }

    private String nullable(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(field.toString());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(current);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("El archivo CSV contiene comillas sin cerrar.");
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
