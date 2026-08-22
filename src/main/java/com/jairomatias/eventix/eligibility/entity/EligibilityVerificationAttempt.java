package com.jairomatias.eventix.eligibility.entity;

import java.time.LocalDateTime;

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
@Table(name = "eligibility_verification_attempts")
public class EligibilityVerificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private SchoolPromotion promotion;

    @Column(name = "national_id_lookup", nullable = false, length = 64)
    private String nationalIdLookup;

    @Column(name = "national_id_last4", nullable = false, length = 4)
    private String nationalIdLast4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationAttemptResult result;

    @Column(length = 500)
    private String reason;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    protected EligibilityVerificationAttempt() {
    }

    public EligibilityVerificationAttempt(
            User user,
            SchoolPromotion promotion,
            String nationalIdLookup,
            String nationalIdLast4,
            VerificationAttemptResult result,
            String reason) {
        this.user = user;
        this.promotion = promotion;
        this.nationalIdLookup = nationalIdLookup;
        this.nationalIdLast4 = nationalIdLast4;
        this.result = result;
        this.reason = reason;
        this.attemptedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public SchoolPromotion getPromotion() {
        return promotion;
    }

    public String getNationalIdLookup() {
        return nationalIdLookup;
    }

    public String getNationalIdLast4() {
        return nationalIdLast4;
    }

    public VerificationAttemptResult getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }
}
