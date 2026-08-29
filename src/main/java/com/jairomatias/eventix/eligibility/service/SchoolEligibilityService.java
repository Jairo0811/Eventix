package com.jairomatias.eventix.eligibility.service;

import java.util.List;

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
    private final CitizenIdentityProvider citizenIdentityProvider;
    private final PersonNameNormalizer nameNormalizer;
    private final EligibilityVerificationRepository verificationRepository;
    private final EligibilityVerificationAttemptRepository attemptRepository;
    private final SchoolPromotionRepository schoolPromotionRepository;
    private final UserRepository userRepository;
    private final SchoolPromotionMembershipSyncService membershipSyncService;

    public SchoolEligibilityService(
            PromotionMemberRepository promotionMemberRepository,
            NationalIdLookupService nationalIdLookupService,
            CitizenIdentityProvider citizenIdentityProvider,
            PersonNameNormalizer nameNormalizer,
            EligibilityVerificationRepository verificationRepository,
            EligibilityVerificationAttemptRepository attemptRepository,
            SchoolPromotionRepository schoolPromotionRepository,
            UserRepository userRepository,
            SchoolPromotionMembershipSyncService membershipSyncService) {
        this.promotionMemberRepository = promotionMemberRepository;
        this.nationalIdLookupService = nationalIdLookupService;
        this.citizenIdentityProvider = citizenIdentityProvider;
        this.nameNormalizer = nameNormalizer;
        this.verificationRepository = verificationRepository;
        this.attemptRepository = attemptRepository;
        this.schoolPromotionRepository = schoolPromotionRepository;
        this.userRepository = userRepository;
        this.membershipSyncService = membershipSyncService;
    }

    @Transactional(readOnly = true)
    public SchoolEligibilityResult verifyPromotionMember(Long promotionId, String nationalId) {
        SchoolPromotion promotion = schoolPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("La promoción no existe."));
        String last4 = nationalIdLookupService.last4(nationalId);
        CitizenIdentityLookupResult identity = citizenIdentityProvider.lookupByNationalId(nationalId);
        if (identity.status() == CitizenIdentityLookupResult.Status.UNAVAILABLE) {
            return SchoolEligibilityResult.identityProviderUnavailable();
        }
        if (identity.status() == CitizenIdentityLookupResult.Status.NOT_FOUND) {
            return SchoolEligibilityResult.identityNotFound();
        }
        List<PromotionMember> matches = matchingMembers(promotionId, identity.fullName());
        if (matches.isEmpty()) {
            return SchoolEligibilityResult.notFound();
        }
        PromotionMember member = matches.getFirst();
        return matches.size() == 1
                ? verifiedResult(member, last4)
                : SchoolEligibilityResult.manualReview(
                        member.getFullName(), promotion.getName(), promotion.getGraduationYear(), last4);
    }

    @Transactional
    public SchoolEligibilityResult verifyAndPersist(Long userId, Long promotionId, String nationalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        SchoolPromotion promotion = schoolPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("La promoción no existe."));
        if (!promotion.isActive() || !promotion.getInstitution().isActive()) {
            throw new IllegalArgumentException("La promoción seleccionada no está disponible para verificación.");
        }

        String lookup = nationalIdLookupService.lookupKey(nationalId);
        String last4 = nationalIdLookupService.last4(nationalId);
        CitizenIdentityLookupResult identity = citizenIdentityProvider.lookupByNationalId(nationalId);

        if (identity.status() == CitizenIdentityLookupResult.Status.UNAVAILABLE) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.PROVIDER_UNAVAILABLE,
                    "La fuente de identidad no está disponible en este momento.");
            return SchoolEligibilityResult.identityProviderUnavailable();
        }
        if (identity.status() == CitizenIdentityLookupResult.Status.NOT_FOUND) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.IDENTITY_NOT_FOUND,
                    "La cédula no pudo resolverse en la fuente de identidad configurada.");
            return SchoolEligibilityResult.identityNotFound();
        }

        List<PromotionMember> matches = matchingMembers(promotionId, identity.fullName());
        if (matches.isEmpty()) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.NO_MATCH,
                    "El nombre oficial asociado a la cédula no figura en el padrón autorizado.");
            return SchoolEligibilityResult.notFound();
        }

        PromotionMember member = matches.getFirst();
        EligibilityVerification existing = verificationRepository
                .findByUser_IdAndPromotionMember_Id(userId, member.getId())
                .orElse(null);

        if (existing != null && existing.getStatus() == VerificationStatus.VERIFIED) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.VERIFIED,
                    "La pertenencia ya estaba verificada.");
            membershipSyncService.syncVerifiedUser(userId, promotionId);
            return verifiedResult(member, last4);
        }

        if (existing != null
                && (existing.getStatus() == VerificationStatus.REJECTED
                        || existing.getStatus() == VerificationStatus.REVOKED)) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.REJECTED,
                    "La verificación fue rechazada o revocada previamente.");
            return SchoolEligibilityResult.rejected(existing.getStatus().name());
        }

        if (matches.size() > 1
                || (existing != null && existing.getStatus() == VerificationStatus.MANUAL_REVIEW)) {
            String reason = matches.size() > 1
                    ? "El nombre oficial coincide con más de un registro del padrón y requiere revisión manual."
                    : "La verificación permanece en revisión manual.";
            EligibilityVerification verification = existing == null
                    ? new EligibilityVerification(user, member, VerificationStatus.MANUAL_REVIEW,
                            VerificationMethod.MANUAL_REVIEW, reason)
                    : existing;
            verification.sendToManualReview(reason);
            verificationRepository.save(verification);
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.MANUAL_REVIEW,
                    verification.getReason());
            return SchoolEligibilityResult.manualReview(
                    member.getFullName(), promotion.getName(), promotion.getGraduationYear(), last4);
        }

        EligibilityVerification verification = existing == null
                ? new EligibilityVerification(user, member, VerificationStatus.PENDING,
                        VerificationMethod.NATIONAL_ID, null)
                : existing;
        verification.verifyAutomatically(
                "El nombre oficial asociado a la cédula coincide exactamente con el padrón autorizado.");
        verificationRepository.save(verification);
        recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.VERIFIED,
                verification.getReason());
        membershipSyncService.syncVerifiedUser(userId, promotionId);
        return verifiedResult(member, last4);
    }

    private List<PromotionMember> matchingMembers(Long promotionId, String officialName) {
        String normalizedOfficialName = nameNormalizer.normalize(officialName);
        return promotionMemberRepository
                .findAllByPromotion_IdAndActiveTrueOrderByFullNameAsc(promotionId)
                .stream()
                .filter(member -> nameNormalizer.normalize(member.getFullName())
                        .equals(normalizedOfficialName))
                .toList();
    }

    private void recordAttempt(User user, SchoolPromotion promotion, String lookup, String last4,
            VerificationAttemptResult result, String reason) {
        attemptRepository.save(new EligibilityVerificationAttempt(
                user, promotion, lookup, last4, result, reason));
    }

    private SchoolEligibilityResult verifiedResult(PromotionMember member, String last4) {
        return SchoolEligibilityResult.verified(
                member.getFullName(),
                member.getPromotion().getName(),
                member.getPromotion().getGraduationYear(),
                last4);
    }
}
