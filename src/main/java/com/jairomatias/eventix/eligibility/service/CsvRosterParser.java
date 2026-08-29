package com.jairomatias.eventix.eligibility.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.jairomatias.eventix.eligibility.dto.RosterImportRow;

@Component
public class CsvRosterParser {

    private static final List<String> EXPECTED_HEADER = List.of(
            "full_name",
            "student_code",
            "source_reference");

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
        if (!header.equals(EXPECTED_HEADER)) {
            throw new IllegalArgumentException(
                    "Encabezado inválido. Use: full_name,student_code,source_reference");
        }

        List<RosterImportRow> result = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (row.size() != EXPECTED_HEADER.size()) {
                throw new IllegalArgumentException(
                        "La fila " + (index + 1) + " no contiene exactamente 3 columnas.");
            }
            result.add(new RosterImportRow(
                    index + 1,
                    row.get(0).trim(),
                    nullable(row.get(1)),
                    nullable(row.get(2))));
        }
        return result;
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
