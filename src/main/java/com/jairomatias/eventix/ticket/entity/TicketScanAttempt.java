package com.jairomatias.eventix.ticket.entity;

import java.time.LocalDateTime;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_scan_attempts")
public class TicketScanAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private DigitalTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "raw_code_hash", nullable = false, length = 64)
    private String rawCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ScanOutcome outcome;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scanned_by_id", nullable = false)
    private User scannedBy;

    @Column(name = "device_identifier", nullable = false, length = 120)
    private String deviceIdentifier;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "first_access", nullable = false)
    private boolean firstAccess;

    @Column(name = "duplicate_attempt", nullable = false)
    private boolean duplicateAttempt;

    @Column(length = 300)
    private String notes;

    protected TicketScanAttempt() {
    }

    public TicketScanAttempt(
            DigitalTicket ticket,
            String rawCodeHash,
            ScanOutcome outcome,
            LocalDateTime occurredAt,
            User scannedBy,
            String deviceIdentifier,
            String ipAddress,
            boolean firstAccess,
            boolean duplicateAttempt,
            String notes) {
        this.ticket = ticket;
        this.event = ticket == null ? null : ticket.getEvent();
        this.rawCodeHash = rawCodeHash;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
        this.scannedBy = scannedBy;
        this.deviceIdentifier = deviceIdentifier;
        this.ipAddress = ipAddress;
        this.firstAccess = firstAccess;
        this.duplicateAttempt = duplicateAttempt;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public DigitalTicket getTicket() {
        return ticket;
    }

    public Event getEvent() {
        return event;
    }

    public ScanOutcome getOutcome() {
        return outcome;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public User getScannedBy() {
        return scannedBy;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isFirstAccess() {
        return firstAccess;
    }

    public boolean isDuplicateAttempt() {
        return duplicateAttempt;
    }

    public String getNotes() {
        return notes;
    }
}
