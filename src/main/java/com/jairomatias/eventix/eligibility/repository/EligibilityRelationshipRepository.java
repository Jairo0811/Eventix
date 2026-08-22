package com.jairomatias.eventix.eligibility.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.eligibility.entity.EligibilityRelationship;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationshipStatus;

import jakarta.persistence.LockModeType;

public interface EligibilityRelationshipRepository
        extends JpaRepository<EligibilityRelationship, Long> {

    boolean existsByGroup_IdAndSponsorUser_IdAndRelatedUser_IdAndStatusIn(
            Long groupId,
            Long sponsorUserId,
            Long relatedUserId,
            Collection<EligibilityRelationshipStatus> statuses);

    long countByGroup_IdAndSponsorUser_IdAndStatus(
            Long groupId,
            Long sponsorUserId,
            EligibilityRelationshipStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from EligibilityRelationship r
            join fetch r.group g
            join fetch g.event e
            join fetch e.organizer
            join fetch r.sponsorUser
            join fetch r.relatedUser
            where r.id = :id
            """)
    Optional<EligibilityRelationship> findDetailedByIdForUpdate(@Param("id") Long id);
}
