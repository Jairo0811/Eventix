package com.jairomatias.eventix.eligibility.dto;

import java.util.List;

public record RosterImportResult(
        Long importId,
        int totalRows,
        int acceptedRows,
        int rejectedRows,
        List<RosterImportError> errors) {
}
