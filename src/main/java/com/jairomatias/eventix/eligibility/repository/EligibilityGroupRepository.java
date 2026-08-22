package com.jairomatias.eventix.eligibility.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;

import jakarta.persistence.LockModeType;

public interface EligibilityGroupRepository extends JpaRepository<EligibilityGroup, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from EligibilityGroup g join fetch g.event e join fetch e.organizer where g.id = :id")
    Optional<EligibilityGroup> findDetailedByIdForUpdate(@Param("id") Long id);
}
