package com.jairomatias.eventix.eligibility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.entity.EligibilityVerification;
import com.jairomatias.eventix.eligibility.entity.VerificationMethod;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class EligibilityManualReviewService {

    private final EligibilityVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public EligibilityManualReviewService(
            EligibilityVerificationRepository verificationRepository,
            UserRepository userRepository) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void approve(Long verificationId, Long reviewerId, String reason) {
        EligibilityVerification verification = getVerification(verificationId);
        User reviewer = getAuthorizedReviewer(reviewerId);
        verification.approve(
                reviewer,
                VerificationMethod.MANUAL_REVIEW,
                requireReason(reason));
        verificationRepository.save(verification);
    }

    @Transactional
    public void reject(Long verificationId, Long reviewerId, String reason) {
        EligibilityVerification verification = getVerification(verificationId);
        User reviewer = getAuthorizedReviewer(reviewerId);
        verification.reject(reviewer, requireReason(reason));
        verificationRepository.save(verification);
    }

    @Transactional
    public void revoke(Long verificationId, Long reviewerId, String reason) {
        EligibilityVerification verification = getVerification(verificationId);
        User reviewer = getAuthorizedReviewer(reviewerId);
        verification.revoke(reviewer, requireReason(reason));
        verificationRepository.save(verification);
    }

    private EligibilityVerification getVerification(Long verificationId) {
        return verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La verificación indicada no existe."));
    }

    private User getAuthorizedReviewer(Long reviewerId) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario revisor no existe."));
        if (reviewer.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new IllegalArgumentException(
                    "Solo un administrador autorizado puede revisar elegibilidad manualmente.");
        }
        return reviewer;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe registrar una justificación para la revisión manual.");
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException(
                    "La justificación no puede superar 500 caracteres.");
        }
        return normalized;
    }
}
