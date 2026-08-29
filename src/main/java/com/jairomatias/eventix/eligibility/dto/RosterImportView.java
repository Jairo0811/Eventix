package com.jairomatias.eventix.eligibility.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.eligibility.entity.SchoolRosterImport;

public record RosterImportView(
        Long id,
        String sourceName,
        String importedBy,
        LocalDateTime importedAt,
        int totalRows,
        int acceptedRows,
        int rejectedRows) {

    public static RosterImportView from(SchoolRosterImport rosterImport) {
        return new RosterImportView(
                rosterImport.getId(),
                rosterImport.getSourceName(),
                rosterImport.getImportedBy().getFullName(),
                rosterImport.getImportedAt(),
                rosterImport.getTotalRows(),
                rosterImport.getAcceptedRows(),
                rosterImport.getRejectedRows());
    }
}
