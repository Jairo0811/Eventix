package com.jairomatias.eventix.eligibility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;

public interface PromotionMemberRepository extends JpaRepository<PromotionMember, Long> {

    Optional<PromotionMember> findByPromotion_IdAndNationalIdLookupAndActiveTrue(
            Long promotionId,
            String nationalIdLookup);

    boolean existsByPromotion_IdAndNationalIdLookup(
            Long promotionId,
            String nationalIdLookup);

    @EntityGraph(attributePaths = {"promotion", "promotion.institution"})
    List<PromotionMember> findAllByPromotion_IdOrderByFullNameAsc(Long promotionId);
}
