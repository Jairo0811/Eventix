package com.jairomatias.eventix.institution.entity;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "institution_memberships", uniqueConstraints = @UniqueConstraint(
        name = "UQ_institution_memberships_institution_user",
        columnNames = {"institution_id", "user_id"}))
public class InstitutionMembership extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private SchoolInstitution institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstitutionMembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstitutionMembershipStatus status = InstitutionMembershipStatus.ACTIVE;

    protected InstitutionMembership() {
    }

    public InstitutionMembership(
            SchoolInstitution institution,
            User user,
            InstitutionMembershipRole role) {
        if (institution == null || user == null || role == null) {
            throw new IllegalArgumentException("Institución, usuario y rol son obligatorios.");
        }
        this.institution = institution;
        this.user = user;
        this.role = role;
    }

    public void changeRole(InstitutionMembershipRole role) {
        if (role == null) {
            throw new IllegalArgumentException("El rol institucional es obligatorio.");
        }
        this.role = role;
    }

    public void activate() {
        this.status = InstitutionMembershipStatus.ACTIVE;
    }

    public void suspend() {
        this.status = InstitutionMembershipStatus.SUSPENDED;
    }

    public SchoolInstitution getInstitution() {
        return institution;
    }

    public User getUser() {
        return user;
    }

    public InstitutionMembershipRole getRole() {
        return role;
    }

    public InstitutionMembershipStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == InstitutionMembershipStatus.ACTIVE;
    }
}
