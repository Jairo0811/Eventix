package com.jairomatias.eventix.eligibility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;

public interface EligibilityMembershipRepository extends JpaRepository<EligibilityMembership, Long> {

    List<EligibilityMembership> findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrueAndGroup_ActiveTrue(
            Long eventId,
            Long userId,
            EligibilityMembershipStatus status);

    boolean existsByGroup_Event_IdAndUser_IdAndStatusAndActiveTrueAndGroup_GroupTypeNot(
            Long eventId,
            Long userId,
            EligibilityMembershipStatus status,
            EligibilityGroupType excludedGroupType);

    Optional<EligibilityMembership> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    @EntityGraph(attributePaths = {"group", "group.event", "group.event.organizer", "user", "sponsorUser"})
    List<EligibilityMembership> findAllByGroup_IdOrderByUser_LastNameAscUser_FirstNameAsc(Long groupId);

    @EntityGraph(attributePaths = {"group", "group.schoolPromotion", "user"})
    List<EligibilityMembership> findAllByGroup_SchoolPromotion_IdAndUser_Id(Long promotionId, Long userId);
}
