package com.jairomatias.eventix.eligibility.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.VerificationStatus;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class SchoolPromotionMembershipSyncService {

    private final EligibilityGroupRepository groupRepository;
    private final EligibilityMembershipRepository membershipRepository;
    private final EligibilityVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public SchoolPromotionMembershipSyncService(
            EligibilityGroupRepository groupRepository,
            EligibilityMembershipRepository membershipRepository,
            EligibilityVerificationRepository verificationRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void syncVerifiedUser(Long userId, Long promotionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        groupRepository.findAllBySchoolPromotion_IdAndGroupTypeAndActiveTrue(
                        promotionId, EligibilityGroupType.PROMOTION_MEMBER)
                .forEach(group -> ensureVerifiedMembership(group, user));
    }

    @Transactional
    public void syncGroup(Long groupId) {
        EligibilityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("El grupo de elegibilidad no existe."));
        if (!group.isActive()
                || group.getGroupType() != EligibilityGroupType.PROMOTION_MEMBER
                || group.getSchoolPromotion() == null) {
            return;
        }
        verificationRepository.findAllByPromotionMember_Promotion_IdAndStatus(
                        group.getSchoolPromotion().getId(), VerificationStatus.VERIFIED)
                .forEach(verification -> ensureVerifiedMembership(group, verification.getUser()));
    }

    @Transactional
    public void revokeForPromotion(Long userId, Long promotionId) {
        membershipRepository.findAllByGroup_SchoolPromotion_IdAndUser_Id(promotionId, userId)
                .forEach(membership -> {
                    membership.revoke();
                    membershipRepository.save(membership);
                });
    }

    private void ensureVerifiedMembership(EligibilityGroup group, User user) {
        EligibilityMembership membership = membershipRepository.findByGroup_IdAndUser_Id(
                        group.getId(), user.getId())
                .orElseGet(() -> new EligibilityMembership(group, user, null));
        membership.verify(null, LocalDateTime.now());
        membershipRepository.save(membership);
    }
}
