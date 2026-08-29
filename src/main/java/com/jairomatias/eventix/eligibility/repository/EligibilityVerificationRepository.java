package com.jairomatias.eventix.eligibility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.EligibilityVerification;
import com.jairomatias.eventix.eligibility.entity.VerificationStatus;

public interface EligibilityVerificationRepository
        extends JpaRepository<EligibilityVerification, Long> {

    Optional<EligibilityVerification> findByUser_IdAndPromotionMember_Id(
            Long userId,
            Long promotionMemberId);

    @EntityGraph(attributePaths = {
            "user", "promotionMember", "promotionMember.promotion",
            "promotionMember.promotion.institution", "verifiedBy"})
    List<EligibilityVerification> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "promotionMember", "promotionMember.promotion"})
    List<EligibilityVerification> findAllByPromotionMember_Promotion_IdAndStatus(
            Long promotionId,
            VerificationStatus status);
}
