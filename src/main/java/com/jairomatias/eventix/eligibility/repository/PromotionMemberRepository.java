package com.jairomatias.eventix.eligibility.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;

public interface PromotionMemberRepository extends JpaRepository<PromotionMember, Long> {

    Optional<PromotionMember> findByPromotionIdAndNationalIdLookupAndActiveTrue(
            Long promotionId,
            String nationalIdLookup);
}
