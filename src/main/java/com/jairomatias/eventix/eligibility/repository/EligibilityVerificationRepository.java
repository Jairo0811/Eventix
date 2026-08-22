package com.jairomatias.eventix.eligibility.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.EligibilityVerification;

public interface EligibilityVerificationRepository
        extends JpaRepository<EligibilityVerification, Long> {

    Optional<EligibilityVerification> findByUser_IdAndPromotionMember_Id(
            Long userId,
            Long promotionMemberId);
}
