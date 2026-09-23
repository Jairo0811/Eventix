package com.jairomatias.eventix.security;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "external_identities",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "UQ_external_identities_provider_subject",
                    columnNames = {"provider", "provider_subject"}),
            @UniqueConstraint(
                    name = "UQ_external_identities_user_provider",
                    columnNames = {"user_id", "provider"})
        })
public class ExternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ExternalIdentity() {
    }

    public ExternalIdentity(User user, String provider, String providerSubject) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
    }

    public User getUser() {
        return user;
    }
}
