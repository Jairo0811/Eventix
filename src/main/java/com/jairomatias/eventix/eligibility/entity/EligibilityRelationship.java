package com.jairomatias.eventix.eligibility.entity;

import java.time.LocalDateTime;

import com.jairomatias.eventix.shared.entity.AuditableEntity;
import com.jairomatias.eventix.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "eligibility_relationships")
public class EligibilityRelationship extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private EligibilityGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sponsor_user_id", nullable = false)
    private User sponsorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_user_id", nullable = false)
    private User relatedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    private EligibilityRelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EligibilityRelationshipStatus status = EligibilityRelationshipStatus.PENDING;

    @Column(name = "request_note", length = 500)
    private String requestNote;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected EligibilityRelationship() {
    }

    public EligibilityRelationship(
            EligibilityGroup group,
            User sponsorUser,
            User relatedUser,
            EligibilityRelationshipType relationshipType,
            String requestNote) {
        if (group == null || sponsorUser == null || relatedUser == null || relationshipType == null) {
            throw new IllegalArgumentException("La relación de elegibilidad requiere grupo, usuarios y tipo.");
        }
        if (sponsorUser.getId() != null && sponsorUser.getId().equals(relatedUser.getId())) {
            throw new IllegalArgumentException("Un usuario no puede patrocinarse a sí mismo como familiar.");
        }
        this.group = group;
        this.sponsorUser = sponsorUser;
        this.relatedUser = relatedUser;
        this.relationshipType = relationshipType;
        this.requestNote = normalizeOptional(requestNote);
    }

    public void approve(User reviewer, String reason, LocalDateTime at) {
        requirePending();
        status = EligibilityRelationshipStatus.APPROVED;
        decidedBy = reviewer;
        decisionReason = requireReason(reason);
        decidedAt = at;
        revokedAt = null;
    }

    public void reject(User reviewer, String reason, LocalDateTime at) {
        requirePending();
        status = EligibilityRelationshipStatus.REJECTED;
        decidedBy = reviewer;
        decisionReason = requireReason(reason);
        decidedAt = at;
    }

    public void revoke(User reviewer, String reason, LocalDateTime at) {
        if (status != EligibilityRelationshipStatus.APPROVED) {
            throw new IllegalStateException("Solo una relación aprobada puede revocarse.");
        }
        status = EligibilityRelationshipStatus.REVOKED;
        decidedBy = reviewer;
        decisionReason = requireReason(reason);
        revokedAt = at;
    }

    private void requirePending() {
        if (status != EligibilityRelationshipStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya fue revisada.");
        }
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Debe registrar una justificación.");
        }
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("La justificación no puede superar 500 caracteres.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("La nota no puede superar 500 caracteres.");
        }
        return normalized;
    }

    public EligibilityGroup getGroup() {
        return group;
    }

    public User getSponsorUser() {
        return sponsorUser;
    }

    public User getRelatedUser() {
        return relatedUser;
    }

    public EligibilityRelationshipType getRelationshipType() {
        return relationshipType;
    }

    public EligibilityRelationshipStatus getStatus() {
        return status;
    }

    public String getRequestNote() {
        return requestNote;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
}
