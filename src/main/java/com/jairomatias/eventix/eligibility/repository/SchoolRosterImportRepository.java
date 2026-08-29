package com.jairomatias.eventix.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.SchoolRosterImport;

public interface SchoolRosterImportRepository extends JpaRepository<SchoolRosterImport, Long> {

    boolean existsByPromotion_IdAndFileChecksum(Long promotionId, String fileChecksum);

    @EntityGraph(attributePaths = {"promotion", "promotion.institution", "importedBy"})
    List<SchoolRosterImport> findAllByPromotion_IdOrderByImportedAtDesc(Long promotionId);
}
