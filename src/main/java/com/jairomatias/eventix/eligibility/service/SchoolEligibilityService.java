package com.jairomatias.eventix.eligibility.service;

import java.text.Normalizer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.SchoolEligibilityResult;
import com.jairomatias.eventix.eligibility.entity.EligibilityVerification;
import com.jairomatias.eventix.eligibility.entity.EligibilityVerificationAttempt;
import com.jairomatias.eventix.eligibility.entity.PromotionMember;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.entity.VerificationAttemptResult;
import com.jairomatias.eventix.eligibility.entity.VerificationMethod;
import com.jairomatias.eventix.eligibility.entity.VerificationStatus;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationAttemptRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationRepository;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class SchoolEligibilityService {

    private final PromotionMemberRepository promotionMemberRepository;
    private final NationalIdLookupService nationalIdLookupService;
    private final EligibilityVerificationRepository verificationRepository;
    private final EligibilityVerificationAttemptRepository attemptRepository;
    private final SchoolPromotionRepository schoolPromotionRepository;
    private final UserRepository userRepository;

    public SchoolEligibilityService(
            PromotionMemberRepository promotionMemberRepository,
            NationalIdLookupService nationalIdLookupService,
            EligibilityVerificationRepository verificationRepository,
            EligibilityVerificationAttemptRepository attemptRepository,
            SchoolPromotionRepository schoolPromotionRepository,
            UserRepository userRepository) {
        this.promotionMemberRepository = promotionMemberRepository;
        this.nationalIdLookupService = nationalIdLookupService;
        this.verificationRepository = verificationRepository;
        this.attemptRepository = attemptRepository;
        this.schoolPromotionRepository = schoolPromotionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SchoolEligibilityResult verifyPromotionMember(Long promotionId, String nationalId) {
        String lookup = nationalIdLookupService.lookupKey(nationalId);
        PromotionMember member = promotionMemberRepository
                .findByPromotion_IdAndNationalIdLookupAndActiveTrue(promotionId, lookup)
                .orElse(null);

        if (member == null) {
            return SchoolEligibilityResult.notFound();
        }

        return verifiedResult(member);
    }

    @Transactional
    public SchoolEligibilityResult verifyAndPersist(
            Long userId,
            Long promotionId,
            String nationalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        SchoolPromotion promotion = schoolPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("La promoción no existe."));

        String lookup = nationalIdLookupService.lookupKey(nationalId);
        String last4 = nationalIdLookupService.last4(nationalId);
        PromotionMember member = promotionMemberRepository
                .findByPromotion_IdAndNationalIdLookupAndActiveTrue(promotionId, lookup)
                .orElse(null);

        if (member == null) {
            recordAttempt(
                    user,
                    promotion,
                    lookup,
                    last4,
                    VerificationAttemptResult.NO_MATCH,
                    "La cédula no coincide con el padrón autorizado.");
            return SchoolEligibilityResult.notFound();
        }

        EligibilityVerification existing = verificationRepository
                .findByUser_IdAndPromotionMember_Id(userId, member.getId())
                .orElse(null);

        if (existing != null && existing.getStatus() == VerificationStatus.VERIFIED) {
            recordAttempt(
                    user,
                    promotion,
                    lookup,
                    last4,
                    VerificationAttemptResult.VERIFIED,
                    "La pertenencia ya estaba verificada.");
            return verifiedResult(member);
        }

        if (existing != null
                && (existing.getStatus() == VerificationStatus.REJECTED
                        || existing.getStatus() == VerificationStatus.REVOKED)) {
            recordAttempt(
                    user,
                    promotion,
                    lookup,
                    last4,
                    VerificationAttemptResult.REJECTED,
                    "La verificación fue rechazada o revocada previamente.");
            return SchoolEligibilityResult.rejected(existing.getStatus().name());
        }

        boolean nameMatches = normalizeName(user.getFullName())
                .equals(normalizeName(member.getFullName()));

        if (!nameMatches || (existing != null
                && existing.getStatus() == VerificationStatus.MANUAL_REVIEW)) {
            EligibilityVerification verification = existing == null
                    ? new EligibilityVerification(
                            user,
                            member,
                            VerificationStatus.MANUAL_REVIEW,
                            VerificationMethod.MANUAL_REVIEW,
                            "La cédula coincide, pero el nombre requiere revisión manual.")
                    : existing;
            verification.sendToManualReview(
                    "La cédula coincide, pero el nombre requiere revisión manual.");
            verificationRepository.save(verification);
            recordAttempt(
                    user,
                    promotion,
                    lookup,
                    last4,
                    VerificationAttemptResult.MANUAL_REVIEW,
                    verification.getReason());
            return SchoolEligibilityResult.manualReview(
                    member.getFullName(),
                    promotion.getName(),
                    promotion.getGraduationYear(),
                    member.getNationalIdLast4());
        }

        EligibilityVerification verification = existing == null
                ? new EligibilityVerification(
                        user,
                        member,
                        VerificationStatus.PENDING,
                        VerificationMethod.NATIONAL_ID,
                        null)
                : existing;
        verification.verifyAutomatically(
                "Cédula y nombre coinciden con el padrón autorizado de la institución.");
        verificationRepository.save(verification);
        recordAttempt(
                user,
                promotion,
                lookup,
                last4,
                VerificationAttemptResult.VERIFIED,
                verification.getReason());
        return verifiedResult(member);
    }

    private void recordAttempt(
            User user,
            SchoolPromotion promotion,
            String lookup,
            String last4,
            VerificationAttemptResult result,
            String reason) {
        attemptRepository.save(new EligibilityVerificationAttempt(
                user,
                promotion,
                lookup,
                last4,
                result,
                reason));
    }

    private SchoolEligibilityResult verifiedResult(PromotionMember member) {
        return SchoolEligibilityResult.verified(
                member.getFullName(),
                member.getPromotion().getName(),
                member.getPromotion().getGraduationYear(),
                member.getNationalIdLast4());
    }

    private String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("\\s+", " ");
    }
}
