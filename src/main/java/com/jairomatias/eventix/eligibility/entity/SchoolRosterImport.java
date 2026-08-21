package com.jairomatias.eventix.eligibility.entity;

import java.time.LocalDateTime;

import com.jairomatias.eventix.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_roster_imports")
public class SchoolRosterImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private SchoolPromotion promotion;

    @Column(name = "source_name", nullable = false, length = 240)
    private String sourceName;

    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imported_by", nullable = false)
    private User importedBy;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "accepted_rows", nullable = false)
    private int acceptedRows;

    @Column(name = "rejected_rows", nullable = false)
    private int rejectedRows;

    protected SchoolRosterImport() {
    }

    public SchoolRosterImport(
            SchoolPromotion promotion,
            String sourceName,
            String fileChecksum,
            User importedBy,
            int totalRows,
            int acceptedRows,
            int rejectedRows) {
        this.promotion = promotion;
        this.sourceName = sourceName;
        this.fileChecksum = fileChecksum;
        this.importedBy = importedBy;
        this.importedAt = LocalDateTime.now();
        this.totalRows = totalRows;
        this.acceptedRows = acceptedRows;
        this.rejectedRows = rejectedRows;
    }

    public Long getId() {
        return id;
    }

    public SchoolPromotion getPromotion() {
        return promotion;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public User getImportedBy() {
        return importedBy;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getAcceptedRows() {
        return acceptedRows;
    }

    public int getRejectedRows() {
        return rejectedRows;
    }
}
