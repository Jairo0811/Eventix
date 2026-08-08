package com.jairomatias.eventix.ticket.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "apple_wallet_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_apple_wallet_device_ticket",
                columnNames = {"device_library_identifier", "ticket_id"}))
public class AppleWalletRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private DigitalTicket ticket;

    @Column(name = "device_library_identifier", nullable = false, length = 160)
    private String deviceLibraryIdentifier;

    @Column(name = "push_token", nullable = false, length = 200)
    private String pushToken;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AppleWalletRegistration() {
    }

    public AppleWalletRegistration(
            DigitalTicket ticket,
            String deviceLibraryIdentifier,
            String pushToken,
            LocalDateTime at) {
        this.ticket = ticket;
        this.deviceLibraryIdentifier = deviceLibraryIdentifier;
        this.pushToken = pushToken;
        this.registeredAt = at;
        this.updatedAt = at;
    }

    public void updatePushToken(String value, LocalDateTime at) {
        pushToken = value;
        updatedAt = at;
    }

    public Long getId() {
        return id;
    }

    public DigitalTicket getTicket() {
        return ticket;
    }

    public String getDeviceLibraryIdentifier() {
        return deviceLibraryIdentifier;
    }

    public String getPushToken() {
        return pushToken;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
