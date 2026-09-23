package com.jairomatias.eventix.eligibility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupSystemKey;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;

import jakarta.persistence.LockModeType;

public interface EligibilityGroupRepository extends JpaRepository<EligibilityGroup, Long> {

    @EntityGraph(attributePaths = {"event", "event.organizer", "schoolPromotion", "schoolPromotion.institution"})
    List<EligibilityGroup> findAllByEvent_IdOrderByNameAsc(Long eventId);

    boolean existsByEvent_IdAndNameIgnoreCase(Long eventId, String name);

    boolean existsByEvent_IdAndNameIgnoreCaseAndIdNot(Long eventId, String name, Long id);

    @EntityGraph(attributePaths = {"event", "event.category", "event.organizer", "schoolPromotion", "schoolPromotion.institution"})
    Optional<EligibilityGroup> findByEvent_IdAndSystemKey(
            Long eventId,
            EligibilityGroupSystemKey systemKey);

    @EntityGraph(attributePaths = {"event", "schoolPromotion"})
    List<EligibilityGroup> findAllBySchoolPromotion_IdAndGroupTypeAndActiveTrue(
            Long promotionId,
            EligibilityGroupType groupType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from EligibilityGroup g join fetch g.event e join fetch e.organizer "
            + "left join fetch g.schoolPromotion sp left join fetch sp.institution where g.id = :id")
    Optional<EligibilityGroup> findDetailedByIdForUpdate(@Param("id") Long id);
}
