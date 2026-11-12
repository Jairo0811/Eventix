package com.jairomatias.eventix.institution.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.eligibility.entity.SchoolInstitutionStatus;
import com.jairomatias.eventix.eligibility.repository.SchoolInstitutionRepository;
import com.jairomatias.eventix.institution.dto.InstitutionDashboardView;
import com.jairomatias.eventix.institution.dto.InstitutionMemberForm;
import com.jairomatias.eventix.institution.dto.InstitutionMemberView;
import com.jairomatias.eventix.institution.dto.InstitutionMembershipView;
import com.jairomatias.eventix.institution.dto.InstitutionRegistrationForm;
import com.jairomatias.eventix.institution.entity.InstitutionMembership;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.repository.InstitutionMembershipRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class InstitutionAccountService {

    private final SchoolInstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final InstitutionAuthorizationService authorizationService;

    public InstitutionAccountService(
            SchoolInstitutionRepository institutionRepository,
            InstitutionMembershipRepository membershipRepository,
            UserRepository userRepository,
            InstitutionAuthorizationService authorizationService) {
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public Long register(InstitutionRegistrationForm form, Long userId) {
        User owner = requireActiveUser(userId);
        String code = normalizeCode(form.code());
        if (institutionRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Ya existe una institución con ese código.");
        }
        SchoolInstitution institution = institutionRepository.save(
                SchoolInstitution.pendingRegistration(form.name(), code));
        membershipRepository.save(new InstitutionMembership(
                institution,
                owner,
                InstitutionMembershipRole.OWNER));
        return institution.getId();
    }

    @Transactional(readOnly = true)
    public List<InstitutionMembershipView> listForUser(Long userId) {
        requireActiveUser(userId);
        return membershipRepository.findAllByUser_IdOrderByInstitution_NameAsc(userId).stream()
                .map(InstitutionMembershipView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionDashboardView getDashboard(Long institutionId, Long userId) {
        InstitutionMembership membership = authorizationService
                .requireDirectMembership(institutionId, userId);
        SchoolInstitution institution = membership.getInstitution();
        return new InstitutionDashboardView(
                institution.getId(),
                institution.getName(),
                institution.getCode(),
                institution.getStatus(),
                membership.getRole(),
                institution.isOperational(),
                membership.getRole().canManageTeam(),
                membership.getRole().canManageEvents(),
                membership.getRole().canManageRoster());
    }

    @Transactional(readOnly = true)
    public List<InstitutionMemberView> listMembers(Long institutionId, Long actorId) {
        authorizationService.requireDirectMembership(institutionId, actorId);
        return membershipRepository
                .findAllByInstitution_IdOrderByUser_LastNameAscUser_FirstNameAsc(institutionId)
                .stream()
                .map(InstitutionMemberView::from)
                .toList();
    }

    @Transactional
    public Long addMember(Long institutionId, InstitutionMemberForm form, Long actorId) {
        SchoolInstitution institution = requireInstitution(institutionId);
        authorizationService.requireOperationalRole(
                institution,
                actorId,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN);
        if (form.role() == InstitutionMembershipRole.OWNER) {
            throw new BusinessRuleException(
                    "La propiedad del centro no se transfiere desde la gestión de miembros.");
        }
        User user = userRepository.findByEmailIgnoreCase(form.email().trim())
                .orElseThrow(() -> new BusinessRuleException(
                        "No existe un usuario Eventix con ese correo."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("El usuario seleccionado no está activo.");
        }
        if (membershipRepository.existsByInstitution_IdAndUser_Id(institutionId, user.getId())) {
            throw new BusinessRuleException("Ese usuario ya pertenece al centro educativo.");
        }
        return membershipRepository.save(new InstitutionMembership(
                institution,
                user,
                form.role())).getId();
    }

    @Transactional
    public void changeMemberRole(
            Long institutionId,
            Long membershipId,
            InstitutionMembershipRole role,
            Long actorId) {
        SchoolInstitution institution = requireInstitution(institutionId);
        authorizationService.requireOperationalRole(
                institution,
                actorId,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN);
        InstitutionMembership membership = requireMembershipOfInstitution(membershipId, institutionId);
        if (membership.getRole() == InstitutionMembershipRole.OWNER
                || role == InstitutionMembershipRole.OWNER) {
            throw new BusinessRuleException("El rol OWNER no puede modificarse desde esta pantalla.");
        }
        membership.changeRole(role);
        membershipRepository.save(membership);
    }

    @Transactional
    public void setMemberActive(
            Long institutionId,
            Long membershipId,
            boolean active,
            Long actorId) {
        SchoolInstitution institution = requireInstitution(institutionId);
        authorizationService.requireOperationalRole(
                institution,
                actorId,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN);
        InstitutionMembership membership = requireMembershipOfInstitution(membershipId, institutionId);
        if (membership.getRole() == InstitutionMembershipRole.OWNER) {
            throw new BusinessRuleException("La membresía OWNER no puede suspenderse.");
        }
        if (active) {
            membership.activate();
        } else {
            membership.suspend();
        }
        membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<InstitutionMembershipView> listPendingForAdministrator(Long actorId) {
        authorizationService.requireAdministrator(actorId);
        return institutionRepository
                .findAllByStatusOrderByNameAsc(SchoolInstitutionStatus.PENDING_VERIFICATION)
                .stream()
                .map(institution -> membershipRepository
                        .findAllByInstitution_IdOrderByUser_LastNameAscUser_FirstNameAsc(institution.getId())
                        .stream()
                        .filter(membership -> membership.getRole() == InstitutionMembershipRole.OWNER)
                        .findFirst()
                        .map(InstitutionMembershipView::from)
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public void approve(Long institutionId, Long actorId) {
        authorizationService.requireAdministrator(actorId);
        SchoolInstitution institution = requireInstitution(institutionId);
        institution.approve();
        institutionRepository.save(institution);
    }

    @Transactional
    public void reject(Long institutionId, Long actorId) {
        authorizationService.requireAdministrator(actorId);
        SchoolInstitution institution = requireInstitution(institutionId);
        institution.reject();
        institutionRepository.save(institution);
    }

    @Transactional
    public void suspend(Long institutionId, Long actorId) {
        authorizationService.requireAdministrator(actorId);
        SchoolInstitution institution = requireInstitution(institutionId);
        institution.suspend();
        institutionRepository.save(institution);
    }

    private SchoolInstitution requireInstitution(Long institutionId) {
        return institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el centro educativo."));
    }

    private InstitutionMembership requireMembershipOfInstitution(Long membershipId, Long institutionId) {
        InstitutionMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la membresía institucional."));
        if (!membership.getInstitution().getId().equals(institutionId)) {
            throw new BusinessRuleException("La membresía no pertenece a este centro educativo.");
        }
        return membership;
    }

    private User requireActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("La cuenta debe estar activa.");
        }
        return user;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código institucional es obligatorio.");
        }
        return code.trim().toUpperCase();
    }
}
