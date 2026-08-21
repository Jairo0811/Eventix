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
@Table(name = "eligibility_verifications")
public class EligibilityVerification extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_member_id", nullable = false)
    private PromotionMember promotionMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 30)
    private VerificationMethod verificationMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(length = 500)
    private String reason;

    protected EligibilityVerification() {
    }

    public EligibilityVerification(
            User user,
            PromotionMember promotionMember,
            VerificationStatus status,
            VerificationMethod verificationMethod,
            String reason) {
        this.user = user;
        this.promotionMember = promotionMember;
        this.status = status;
        this.verificationMethod = verificationMethod;
        this.reason = reason;
    }

    public void approve(User reviewer, VerificationMethod method, String approvalReason) {
        status = VerificationStatus.VERIFIED;
        verifiedBy = reviewer;
        verifiedAt = LocalDateTime.now();
        verificationMethod = method;
        reason = approvalReason;
    }

    public void sendToManualReview(String reviewReason) {
        status = VerificationStatus.MANUAL_REVIEW;
        verificationMethod = VerificationMethod.MANUAL_REVIEW;
        verifiedBy = null;
        verifiedAt = null;
        reason = reviewReason;
    }

    public void reject(User reviewer, String rejectionReason) {
        status = VerificationStatus.REJECTED;
        verifiedBy = reviewer;
        verifiedAt = LocalDateTime.now();
        verificationMethod = VerificationMethod.MANUAL_REVIEW;
        reason = rejectionReason;
    }

    public void revoke(User reviewer, String revokeReason) {
        status = VerificationStatus.REVOKED;
        verifiedBy = reviewer;
        verifiedAt = LocalDateTime.now();
        verificationMethod = VerificationMethod.MANUAL_REVIEW;
        reason = revokeReason;
    }

    public User getUser() {
        return user;
    }

    public PromotionMember getPromotionMember() {
        return promotionMember;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public VerificationMethod getVerificationMethod() {
        return verificationMethod;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public String getReason() {
        return reason;
    }
}
