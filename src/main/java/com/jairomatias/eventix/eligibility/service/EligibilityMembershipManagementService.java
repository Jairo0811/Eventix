package com.jairomatias.eventix.eligibility.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.EligibilityMembershipForm;
import com.jairomatias.eventix.eligibility.dto.EligibilityMembershipView;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class EligibilityMembershipManagementService {

    private static final EnumSet<EligibilityGroupType> DIRECT_MANAGEMENT_TYPES = EnumSet.of(
            EligibilityGroupType.STAFF,
            EligibilityGroupType.VIP,
            EligibilityGroupType.CUSTOM);

    private final EligibilityGroupRepository groupRepository;
    private final EligibilityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public EligibilityMembershipManagementService(
            EligibilityGroupRepository groupRepository,
            EligibilityMembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibilityMembershipView> list(Long groupId, Long actorId) {
        EligibilityGroup group = getGroup(groupId);
        authorize(actorId, group.getEvent());
        return membershipRepository.findAllByGroup_IdOrderByUser_LastNameAscUser_FirstNameAsc(groupId)
                .stream()
                .map(EligibilityMembershipView::from)
                .toList();
    }

    @Transactional
    public void addVerified(Long groupId, EligibilityMembershipForm form, Long actorId) {
        EligibilityGroup group = getGroupForUpdate(groupId);
        authorize(actorId, group.getEvent());
        validateDirectManagement(group);
        if (!group.isActive()) {
            throw new BusinessRuleException("No puedes agregar miembros a un grupo inactivo.");
        }
        if (form == null || form.email() == null || form.email().isBlank()) {
            throw new BusinessRuleException("El correo del miembro es obligatorio.");
        }

        User member = userRepository.findByEmailIgnoreCase(form.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ese correo."));
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("El usuario debe estar activo antes de recibir una membresía.");
        }

        EligibilityMembership membership = membershipRepository
                .findByGroup_IdAndUser_Id(groupId, member.getId())
                .orElseGet(() -> new EligibilityMembership(group, member, null));
        if (membership.isActive()
                && membership.getStatus() == com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus.VERIFIED) {
            throw new BusinessRuleException("El usuario ya pertenece a este grupo.");
        }

        membership.verify(null, LocalDateTime.now());
        membershipRepository.save(membership);
    }

    @Transactional
    public void revoke(Long membershipId, Long actorId) {
        EligibilityMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la membresía."));
        authorize(actorId, membership.getGroup().getEvent());
        membership.revoke();
        membershipRepository.save(membership);
    }

    private EligibilityGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo de elegibilidad."));
    }

    private EligibilityGroup getGroupForUpdate(Long groupId) {
        return groupRepository.findDetailedByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo de elegibilidad."));
    }

    private void validateDirectManagement(EligibilityGroup group) {
        if (!DIRECT_MANAGEMENT_TYPES.contains(group.getGroupType())) {
            throw new BusinessRuleException(
                    "Este tipo de grupo exige un flujo de verificación específico y no admite altas manuales directas.");
        }
    }

    private void authorize(Long actorId, Event event) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
        RoleName role = actor.getRole().getName();
        if (role == RoleName.ADMINISTRATOR) {
            return;
        }
        if (role == RoleName.ORGANIZER && event.getOrganizer().getId().equals(actorId)) {
            return;
        }
        throw new BusinessRuleException("No tienes permisos para administrar la elegibilidad de este evento.");
    }
}
