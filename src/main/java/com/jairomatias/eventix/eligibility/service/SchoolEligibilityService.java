package com.jairomatias.eventix.eligibility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.SchoolEligibilityResult;
import com.jairomatias.eventix.eligibility.entity.PromotionMember;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;

@Service
public class SchoolEligibilityService {

    private final PromotionMemberRepository promotionMemberRepository;
    private final NationalIdLookupService nationalIdLookupService;

    public SchoolEligibilityService(
            PromotionMemberRepository promotionMemberRepository,
            NationalIdLookupService nationalIdLookupService) {
        this.promotionMemberRepository = promotionMemberRepository;
        this.nationalIdLookupService = nationalIdLookupService;
    }

    @Transactional(readOnly = true)
    public SchoolEligibilityResult verifyPromotionMember(Long promotionId, String nationalId) {
        String lookup = nationalIdLookupService.lookupKey(nationalId);
        PromotionMember member = promotionMemberRepository
                .findByPromotionIdAndNationalIdLookupAndActiveTrue(promotionId, lookup)
                .orElse(null);

        if (member == null) {
            return SchoolEligibilityResult.notFound();
        }

        return SchoolEligibilityResult.verified(
                member.getFullName(),
                member.getPromotion().getName(),
                member.getPromotion().getGraduationYear(),
                member.getNationalIdLast4());
    }
}
