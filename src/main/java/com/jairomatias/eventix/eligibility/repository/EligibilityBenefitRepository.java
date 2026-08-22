package com.jairomatias.eventix.eligibility.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;

public interface EligibilityBenefitRepository extends JpaRepository<EligibilityBenefit, Long> {

    List<EligibilityBenefit> findAllByGroup_IdInAndActiveTrue(Collection<Long> groupIds);

    List<EligibilityBenefit> findAllByTicketType_IdAndBenefitTypeAndActiveTrue(
            Long ticketTypeId,
            EligibilityBenefitType benefitType);
}
