package com.jairomatias.eventix.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;

public interface PromotionMemberRepository extends JpaRepository<PromotionMember, Long> {

    List<PromotionMember> findAllByPromotion_IdAndNormalizedFullNameAndActiveTrue(
            Long promotionId,
            String normalizedFullName);

    boolean existsByPromotion_IdAndStudentCodeIgnoreCase(Long promotionId, String studentCode);

    @EntityGraph(attributePaths = {"promotion", "promotion.institution"})
    List<PromotionMember> findAllByPromotion_IdOrderByFullNameAsc(Long promotionId);
}
