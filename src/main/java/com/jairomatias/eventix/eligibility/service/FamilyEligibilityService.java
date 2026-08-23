package com.jairomatias.eventix.eligibility.service;

import java.time.LocalDateTime;
import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationship;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationshipStatus;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationshipType;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityRelationshipRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class FamilyEligibilityService {

    private final EligibilityGroupRepository groupRepository;
    private final EligibilityMembershipRepository membershipRepository;
    private final EligibilityRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    public FamilyEligibilityService(
            EligibilityGroupRepository groupRepository,
            EligibilityMembershipRepository membershipRepository,
            EligibilityRelationshipRepository relationshipRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long requestFamilyLink(
            Long familyGroupId,
            Long sponsorUserId,
            Long relatedUserId,
            EligibilityRelationshipType relationshipType,
            String requestNote) {
        EligibilityGroup group = getFamilyGroup(familyGroupId);
        User sponsor = getUser(sponsorUserId);
        User related = getUser(relatedUserId);

        if (sponsor.getId().equals(related.getId())) {
            throw new BusinessRuleException("No puedes registrarte como tu propio familiar.");
        }
        if (!membershipRepository
                .existsByGroup_Event_IdAndUser_IdAndStatusAndActiveTrueAndGroup_GroupTypeNot(
                        group.getEvent().getId(),
                        sponsor.getId(),
                        EligibilityMembershipStatus.VERIFIED,
                        EligibilityGroupType.FAMILY)) {
            throw new BusinessRuleException(
                    "El patrocinador debe tener una elegibilidad verificada en este evento.");
        }
        if (membershipRepository.findByGroup_IdAndUser_Id(group.getId(), related.getId())
                .filter(membership -> membership.isActive()
                        && membership.getStatus() == EligibilityMembershipStatus.VERIFIED)
                .isPresent()) {
            throw new BusinessRuleException("El familiar ya tiene una membresía verificada en este grupo.");
        }
        boolean duplicate = relationshipRepository
                .existsByGroup_IdAndSponsorUser_IdAndRelatedUser_IdAndStatusIn(
                        group.getId(),
                        sponsor.getId(),
                        related.getId(),
                        EnumSet.of(
                                EligibilityRelationshipStatus.PENDING,
                                EligibilityRelationshipStatus.APPROVED));
        if (duplicate) {
            throw new BusinessRuleException(
                    "Ya existe una solicitud pendiente o aprobada para este vínculo familiar.");
        }

        EligibilityRelationship relationship = new EligibilityRelationship(
                group,
                sponsor,
                related,
                requireType(relationshipType),
                requestNote);
        return relationshipRepository.save(relationship).getId();
    }

    @Transactional
    public void approve(Long relationshipId, Long reviewerId, String reason) {
        EligibilityRelationship relationship = getRelationshipForUpdate(relationshipId);
        EligibilityGroup group = groupRepository.findDetailedByIdForUpdate(relationship.getGroup().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El grupo de elegibilidad ya no existe."));
        User reviewer = getAuthorizedReviewer(reviewerId, group);
        enforceSponsorStillEligible(group, relationship.getSponsorUser());
        enforceFamilyLimit(group, relationship.getSponsorUser());

        LocalDateTime now = LocalDateTime.now();
        relationship.approve(reviewer, reason, now);

        EligibilityMembership membership = membershipRepository
                .findByGroup_IdAndUser_Id(group.getId(), relationship.getRelatedUser().getId())
                .orElseGet(() -> new EligibilityMembership(
                        group,
                        relationship.getRelatedUser(),
                        relationship.getSponsorUser()));
        membership.verify(relationship.getSponsorUser(), now);
        membershipRepository.save(membership);
        relationshipRepository.save(relationship);
    }

    @Transactional
    public void reject(Long relationshipId, Long reviewerId, String reason) {
        EligibilityRelationship relationship = getRelationshipForUpdate(relationshipId);
        User reviewer = getAuthorizedReviewer(reviewerId, relationship.getGroup());
        relationship.reject(reviewer, reason, LocalDateTime.now());
        relationshipRepository.save(relationship);
    }

    @Transactional
    public void revoke(Long relationshipId, Long reviewerId, String reason) {
        EligibilityRelationship relationship = getRelationshipForUpdate(relationshipId);
        User reviewer = getAuthorizedReviewer(reviewerId, relationship.getGroup());
        LocalDateTime now = LocalDateTime.now();
        relationship.revoke(reviewer, reason, now);

        membershipRepository
                .findByGroup_IdAndUser_Id(
                        relationship.getGroup().getId(),
                        relationship.getRelatedUser().getId())
                .filter(membership -> isSponsoredBy(membership, relationship.getSponsorUser()))
                .ifPresent(membership -> {
                    membership.revoke();
                    membershipRepository.save(membership);
                });
        relationshipRepository.save(relationship);
    }

    private EligibilityGroup getFamilyGroup(Long groupId) {
        EligibilityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo familiar."));
        validateFamilyGroup(group);
        return group;
    }

    private EligibilityRelationship getRelationshipForUpdate(Long relationshipId) {
        EligibilityRelationship relationship = relationshipRepository.findDetailedByIdForUpdate(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud familiar."));
        validateFamilyGroup(relationship.getGroup());
        return relationship;
    }

    private void validateFamilyGroup(EligibilityGroup group) {
        if (!group.isActive() || group.getGroupType() != EligibilityGroupType.FAMILY) {
            throw new BusinessRuleException("El grupo indicado no admite relaciones familiares activas.");
        }
    }

    private void enforceSponsorStillEligible(EligibilityGroup group, User sponsor) {
        boolean eligible = membershipRepository
                .existsByGroup_Event_IdAndUser_IdAndStatusAndActiveTrueAndGroup_GroupTypeNot(
                        group.getEvent().getId(),
                        sponsor.getId(),
                        EligibilityMembershipStatus.VERIFIED,
                        EligibilityGroupType.FAMILY);
        if (!eligible) {
            throw new BusinessRuleException(
                    "La elegibilidad principal del patrocinador ya no está verificada.");
        }
    }

    private void enforceFamilyLimit(EligibilityGroup group, User sponsor) {
        Integer limit = group.getMaxRelatedPeople();
        if (limit == null) {
            return;
        }
        long approved = relationshipRepository.countByGroup_IdAndSponsorUser_IdAndStatus(
                group.getId(), sponsor.getId(), EligibilityRelationshipStatus.APPROVED);
        if (approved >= limit) {
            throw new BusinessRuleException(
                    "El patrocinador alcanzó el límite de " + limit + " familiares aprobados.");
        }
    }

    private User getAuthorizedReviewer(Long reviewerId, EligibilityGroup group) {
        User reviewer = getUser(reviewerId);
        RoleName role = reviewer.getRole().getName();
        if (role == RoleName.ADMINISTRATOR) {
            return reviewer;
        }
        if (role == RoleName.ORGANIZER
                && group.getEvent().getOrganizer().getId().equals(reviewer.getId())) {
            return reviewer;
        }
        throw new BusinessRuleException(
                "Solo un administrador o el organizador propietario del evento puede revisar este vínculo.");
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario solicitado."));
    }

    private EligibilityRelationshipType requireType(EligibilityRelationshipType type) {
        if (type == null) {
            throw new BusinessRuleException("Debes indicar el tipo de relación familiar.");
        }
        return type;
    }

    private boolean isSponsoredBy(EligibilityMembership membership, User sponsor) {
        return membership.getSponsorUser() != null
                && membership.getSponsorUser().getId().equals(sponsor.getId());
    }
}
