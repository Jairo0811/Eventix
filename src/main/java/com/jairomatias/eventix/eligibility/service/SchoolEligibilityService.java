package com.jairomatias.eventix.eligibility.service;

import java.util.List;
import java.util.Optional;

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
import com.jairomatias.eventix.eligibility.identity.CitizenIdentity;
import com.jairomatias.eventix.eligibility.identity.CitizenIdentityProvider;
import com.jairomatias.eventix.eligibility.identity.CitizenIdentityProviderUnavailableException;
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
    private final PersonNameNormalizer nameNormalizer;
    private final CitizenIdentityProvider identityProvider;
    private final EligibilityVerificationRepository verificationRepository;
    private final EligibilityVerificationAttemptRepository attemptRepository;
    private final SchoolPromotionRepository schoolPromotionRepository;
    private final UserRepository userRepository;
    private final SchoolPromotionMembershipSyncService membershipSyncService;

    public SchoolEligibilityService(
            PromotionMemberRepository promotionMemberRepository,
            NationalIdLookupService nationalIdLookupService,
            PersonNameNormalizer nameNormalizer,
            CitizenIdentityProvider identityProvider,
            EligibilityVerificationRepository verificationRepository,
            EligibilityVerificationAttemptRepository attemptRepository,
            SchoolPromotionRepository schoolPromotionRepository,
            UserRepository userRepository,
            SchoolPromotionMembershipSyncService membershipSyncService) {
        this.promotionMemberRepository = promotionMemberRepository;
        this.nationalIdLookupService = nationalIdLookupService;
        this.nameNormalizer = nameNormalizer;
        this.identityProvider = identityProvider;
        this.verificationRepository = verificationRepository;
        this.attemptRepository = attemptRepository;
        this.schoolPromotionRepository = schoolPromotionRepository;
        this.userRepository = userRepository;
        this.membershipSyncService = membershipSyncService;
    }

    @Transactional(readOnly = true)
    public SchoolEligibilityResult verifyPromotionMember(Long promotionId, String nationalId) {
        String normalizedNationalId = nationalIdLookupService.normalizeNationalId(nationalId);
        Optional<CitizenIdentity> identity;
        try {
            identity = identityProvider.findByNationalId(normalizedNationalId);
        } catch (CitizenIdentityProviderUnavailableException exception) {
            return SchoolEligibilityResult.providerUnavailable();
        }
        if (identity.isEmpty()) {
            return SchoolEligibilityResult.identityNotFound();
        }

        List<PromotionMember> matches = findActiveRosterMatches(
                promotionId,
                identity.get().fullName());
        if (matches.isEmpty()) {
            return SchoolEligibilityResult.notFound();
        }
        if (matches.size() > 1) {
            return SchoolEligibilityResult.ambiguousMatch();
        }
        return verifiedResult(matches.getFirst(), nationalIdLookupService.last4(normalizedNationalId));
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

        String normalizedNationalId = nationalIdLookupService.normalizeNationalId(nationalId);
        String lookup = nationalIdLookupService.lookupKey(normalizedNationalId);
        String last4 = nationalIdLookupService.last4(normalizedNationalId);

        Optional<CitizenIdentity> identity;
        try {
            identity = identityProvider.findByNationalId(normalizedNationalId);
        } catch (CitizenIdentityProviderUnavailableException exception) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.PROVIDER_UNAVAILABLE,
                    "El proveedor autorizado de identidad no está disponible.");
            return SchoolEligibilityResult.providerUnavailable();
        }

        if (identity.isEmpty()) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.IDENTITY_NOT_FOUND,
                    "La cédula no pudo resolverse en el proveedor autorizado de identidad.");
            return SchoolEligibilityResult.identityNotFound();
        }

        List<PromotionMember> matches = findActiveRosterMatches(
                promotionId,
                identity.get().fullName());

        if (matches.isEmpty()) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.NO_MATCH,
                    "El nombre asociado a la cédula no aparece en el padrón autorizado.");
            return SchoolEligibilityResult.notFound();
        }

        if (matches.size() > 1) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.AMBIGUOUS_MATCH,
                    "El nombre asociado a la cédula aparece más de una vez en el padrón.");
            return SchoolEligibilityResult.ambiguousMatch();
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

        if (existing != null && existing.getStatus() == VerificationStatus.MANUAL_REVIEW) {
            recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.MANUAL_REVIEW,
                    existing.getReason());
            return SchoolEligibilityResult.manualReview(
                    member.getFullName(),
                    promotion.getName(),
                    promotion.getGraduationYear(),
                    last4);
        }

        EligibilityVerification verification = existing == null
                ? new EligibilityVerification(user, member, VerificationStatus.PENDING,
                        VerificationMethod.NATIONAL_ID, null)
                : existing;
        verification.verifyAutomatically(
                "El nombre obtenido del proveedor de identidad coincide con el padrón autorizado de la institución.");
        verificationRepository.save(verification);
        recordAttempt(user, promotion, lookup, last4, VerificationAttemptResult.VERIFIED,
                verification.getReason());
        membershipSyncService.syncVerifiedUser(userId, promotionId);
        return verifiedResult(member, last4);
    }

    private List<PromotionMember> findActiveRosterMatches(Long promotionId, String officialName) {
        String normalizedName = nameNormalizer.normalize(officialName);
        return promotionMemberRepository
                .findAllByPromotion_IdAndNormalizedFullNameAndActiveTrue(promotionId, normalizedName);
    }

    private void recordAttempt(User user, SchoolPromotion promotion, String lookup, String last4,
            VerificationAttemptResult result, String reason) {
        attemptRepository.save(new EligibilityVerificationAttempt(
                user, promotion, lookup, last4, result, reason));
    }

    private SchoolEligibilityResult verifiedResult(PromotionMember member, String nationalIdLast4) {
        return SchoolEligibilityResult.verified(
                member.getFullName(),
                member.getPromotion().getName(),
                member.getPromotion().getGraduationYear(),
                nationalIdLast4);
    }
}
