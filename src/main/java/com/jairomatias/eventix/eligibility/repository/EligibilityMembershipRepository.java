package com.jairomatias.eventix.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;

public interface EligibilityMembershipRepository extends JpaRepository<EligibilityMembership, Long> {

    List<EligibilityMembership> findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
            Long eventId,
            Long userId,
            EligibilityMembershipStatus status);
}
