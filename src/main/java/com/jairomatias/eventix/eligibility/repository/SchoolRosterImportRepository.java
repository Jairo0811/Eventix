package com.jairomatias.eventix.eligibility.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.SchoolRosterImport;

public interface SchoolRosterImportRepository extends JpaRepository<SchoolRosterImport, Long> {

    boolean existsByPromotion_IdAndFileChecksum(Long promotionId, String fileChecksum);
}
