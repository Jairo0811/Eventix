package com.jairomatias.eventix.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;

public interface PromotionMemberRepository extends JpaRepository<PromotionMember, Long> {

    @EntityGraph(attributePaths = {"promotion", "promotion.institution"})
    List<PromotionMember> findAllByPromotion_IdOrderByFullNameAsc(Long promotionId);

    @EntityGraph(attributePaths = {"promotion", "promotion.institution"})
    List<PromotionMember> findAllByPromotion_IdAndActiveTrueOrderByFullNameAsc(Long promotionId);
}
