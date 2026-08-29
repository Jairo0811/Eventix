package com.jairomatias.eventix.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;

public interface SchoolPromotionRepository extends JpaRepository<SchoolPromotion, Long> {

    @EntityGraph(attributePaths = {"institution"})
    List<SchoolPromotion> findAllByOrderByGraduationYearDescNameAsc();

    @EntityGraph(attributePaths = {"institution"})
    List<SchoolPromotion> findAllByActiveTrueAndInstitution_ActiveTrueOrderByGraduationYearDescNameAsc();

    @EntityGraph(attributePaths = {"institution"})
    List<SchoolPromotion> findAllByInstitution_IdOrderByGraduationYearDescNameAsc(Long institutionId);

    boolean existsByInstitution_IdAndGraduationYear(Long institutionId, int graduationYear);

    boolean existsByInstitution_IdAndGraduationYearAndIdNot(Long institutionId, int graduationYear, Long id);
}
