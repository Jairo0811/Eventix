package com.jairomatias.eventix.eligibility.dto;

public record RosterImportRow(
        int rowNumber,
        String fullName,
        String studentCode,
        String sourceReference) {
}
