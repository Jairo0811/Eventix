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
@Table(name = "eligibility_memberships")
public class EligibilityMembership extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private EligibilityGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_user_id")
    private User sponsorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EligibilityMembershipStatus status = EligibilityMembershipStatus.PENDING;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    protected EligibilityMembership() {
    }

    public EligibilityMembership(EligibilityGroup group, User user, User sponsorUser) {
        this.group = group;
        this.user = user;
        this.sponsorUser = sponsorUser;
    }

    public void verify(User sponsorUser, LocalDateTime at) {
        this.sponsorUser = sponsorUser;
        this.status = EligibilityMembershipStatus.VERIFIED;
        this.active = true;
        this.verifiedAt = at;
    }

    public void revoke() {
        this.status = EligibilityMembershipStatus.REVOKED;
        this.active = false;
    }

    public EligibilityGroup getGroup() {
        return group;
    }

    public User getUser() {
        return user;
    }

    public User getSponsorUser() {
        return sponsorUser;
    }

    public EligibilityMembershipStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
}
