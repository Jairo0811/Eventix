package com.jairomatias.eventix.eligibility.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion_members")
public class PromotionMember extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private SchoolPromotion promotion;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "normalized_full_name", nullable = false, length = 180)
    private String normalizedFullName;

    @Column(name = "student_code", length = 80)
    private String studentCode;

    @Column(name = "source_reference", length = 240)
    private String sourceReference;

    @Column(nullable = false)
    private boolean active = true;

    protected PromotionMember() {
    }

    public PromotionMember(
            SchoolPromotion promotion,
            String fullName,
            String normalizedFullName,
            String studentCode,
            String sourceReference) {
        this.promotion = promotion;
        this.fullName = fullName;
        this.normalizedFullName = normalizedFullName;
        this.studentCode = studentCode;
        this.sourceReference = sourceReference;
    }

    public SchoolPromotion getPromotion() {
        return promotion;
    }

    public String getFullName() {
        return fullName;
    }

    public String getNormalizedFullName() {
        return normalizedFullName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public boolean isActive() {
        return active;
    }
}
