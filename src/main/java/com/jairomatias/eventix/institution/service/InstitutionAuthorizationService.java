package com.jairomatias.eventix.institution.service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.institution.entity.InstitutionMembership;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipStatus;
import com.jairomatias.eventix.institution.repository.InstitutionMembershipRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class InstitutionAuthorizationService {

    private final InstitutionMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public InstitutionAuthorizationService(
            InstitutionMembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InstitutionMembership requireDirectMembership(Long institutionId, Long userId) {
        InstitutionMembership membership = membershipRepository
                .findByInstitution_IdAndUser_Id(institutionId, userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "No perteneces a este centro educativo."));
        if (membership.getStatus() != InstitutionMembershipStatus.ACTIVE) {
            throw new BusinessRuleException("Tu membresía institucional está suspendida.");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public void requireInstitutionRole(
            SchoolInstitution institution,
            Long userId,
            InstitutionMembershipRole... allowedRoles) {
        if (isAdministrator(userId)) {
            return;
        }
        InstitutionMembership membership = requireDirectMembership(institution.getId(), userId);
        Set<InstitutionMembershipRole> allowed = allowedRoles.length == 0
                ? EnumSet.allOf(InstitutionMembershipRole.class)
                : EnumSet.copyOf(Arrays.asList(allowedRoles));
        if (!allowed.contains(membership.getRole())) {
            throw new BusinessRuleException(
                    "Tu rol institucional no tiene permiso para realizar esta operación.");
        }
    }

    @Transactional(readOnly = true)
    public void requireOperationalRole(
            SchoolInstitution institution,
            Long userId,
            InstitutionMembershipRole... allowedRoles) {
        requireInstitutionRole(institution, userId, allowedRoles);
        if (!institution.isOperational()) {
            throw new BusinessRuleException(
                    "El centro educativo debe estar aprobado y activo para realizar esta operación.");
        }
    }

    @Transactional(readOnly = true)
    public void requireAdministrator(Long userId) {
        if (!isAdministrator(userId)) {
            throw new BusinessRuleException("Solo un administrador de Eventix puede realizar esta operación.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isAdministrator(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
        return user.getRole().getName() == RoleName.ADMINISTRATOR;
    }
}
