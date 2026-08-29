package com.jairomatias.eventix.eligibility.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.PromotionMemberView;
import com.jairomatias.eventix.eligibility.dto.RosterImportView;
import com.jairomatias.eventix.eligibility.dto.SchoolInstitutionForm;
import com.jairomatias.eventix.eligibility.dto.SchoolInstitutionView;
import com.jairomatias.eventix.eligibility.dto.SchoolPromotionForm;
import com.jairomatias.eventix.eligibility.dto.SchoolPromotionView;
import com.jairomatias.eventix.eligibility.dto.SchoolVerificationView;
import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationRepository;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolInstitutionRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolRosterImportRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class SchoolPromotionManagementService {

    private final SchoolInstitutionRepository institutionRepository;
    private final SchoolPromotionRepository promotionRepository;
    private final PromotionMemberRepository memberRepository;
    private final SchoolRosterImportRepository rosterImportRepository;
    private final EligibilityVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public SchoolPromotionManagementService(
            SchoolInstitutionRepository institutionRepository,
            SchoolPromotionRepository promotionRepository,
            PromotionMemberRepository memberRepository,
            SchoolRosterImportRepository rosterImportRepository,
            EligibilityVerificationRepository verificationRepository,
            UserRepository userRepository) {
        this.institutionRepository = institutionRepository;
        this.promotionRepository = promotionRepository;
        this.memberRepository = memberRepository;
        this.rosterImportRepository = rosterImportRepository;
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SchoolInstitutionView> listInstitutions(Long actorId) {
        requireAdministrator(actorId);
        return institutionRepository.findAllByOrderByNameAsc().stream()
                .map(SchoolInstitutionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPromotionView> listPromotions(Long actorId) {
        requireAdministrator(actorId);
        return promotionRepository.findAllByOrderByGraduationYearDescNameAsc().stream()
                .map(SchoolPromotionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPromotionView> listActivePromotions() {
        return promotionRepository.findAllByActiveTrueAndInstitution_ActiveTrueOrderByGraduationYearDescNameAsc()
                .stream()
                .map(SchoolPromotionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolPromotionView getPromotion(Long promotionId, Long actorId) {
        requireAdministrator(actorId);
        return SchoolPromotionView.from(getPromotionEntity(promotionId));
    }

    @Transactional(readOnly = true)
    public List<PromotionMemberView> listMembers(Long promotionId, Long actorId) {
        requireAdministrator(actorId);
        getPromotionEntity(promotionId);
        return memberRepository.findAllByPromotion_IdOrderByFullNameAsc(promotionId).stream()
                .map(PromotionMemberView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RosterImportView> listImports(Long promotionId, Long actorId) {
        requireAdministrator(actorId);
        getPromotionEntity(promotionId);
        return rosterImportRepository.findAllByPromotion_IdOrderByImportedAtDesc(promotionId).stream()
                .map(RosterImportView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolVerificationView> listVerifications(Long actorId) {
        requireAdministrator(actorId);
        return verificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SchoolVerificationView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolVerificationView> listUserVerifications(Long userId) {
        return verificationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(SchoolVerificationView::from)
                .toList();
    }

    @Transactional
    public Long createInstitution(SchoolInstitutionForm form, Long actorId) {
        requireAdministrator(actorId);
        String code = form.code().trim().toUpperCase();
        if (institutionRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Ya existe una institución con ese código.");
        }
        return institutionRepository.save(new SchoolInstitution(form.name(), code)).getId();
    }

    @Transactional
    public void updateInstitution(Long institutionId, SchoolInstitutionForm form, Long actorId) {
        requireAdministrator(actorId);
        SchoolInstitution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la institución."));
        String code = form.code().trim().toUpperCase();
        if (institutionRepository.existsByCodeIgnoreCaseAndIdNot(code, institutionId)) {
            throw new BusinessRuleException("Ya existe otra institución con ese código.");
        }
        institution.update(form.name(), code);
        institutionRepository.save(institution);
    }

    @Transactional
    public void setInstitutionActive(Long institutionId, boolean active, Long actorId) {
        requireAdministrator(actorId);
        SchoolInstitution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la institución."));
        if (active) {
            institution.activate();
        } else {
            institution.deactivate();
        }
        institutionRepository.save(institution);
    }

    @Transactional
    public Long createPromotion(SchoolPromotionForm form, Long actorId) {
        requireAdministrator(actorId);
        SchoolInstitution institution = institutionRepository.findById(form.institutionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la institución."));
        if (!institution.isActive()) {
            throw new BusinessRuleException("La institución debe estar activa para crear promociones.");
        }
        if (promotionRepository.existsByInstitution_IdAndGraduationYear(
                form.institutionId(), form.graduationYear())) {
            throw new BusinessRuleException("Ya existe una promoción para ese año en la institución.");
        }
        SchoolPromotion promotion = new SchoolPromotion(institution, form.name(), form.graduationYear());
        return promotionRepository.save(promotion).getId();
    }

    @Transactional
    public void updatePromotion(Long promotionId, SchoolPromotionForm form, Long actorId) {
        requireAdministrator(actorId);
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        if (!promotion.getInstitution().getId().equals(form.institutionId())) {
            throw new BusinessRuleException("No se puede mover una promoción a otra institución.");
        }
        if (promotionRepository.existsByInstitution_IdAndGraduationYearAndIdNot(
                form.institutionId(), form.graduationYear(), promotionId)) {
            throw new BusinessRuleException("Ya existe otra promoción para ese año en la institución.");
        }
        promotion.update(form.name(), form.graduationYear());
        promotionRepository.save(promotion);
    }

    @Transactional
    public void setPromotionActive(Long promotionId, boolean active, Long actorId) {
        requireAdministrator(actorId);
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        if (active && !promotion.getInstitution().isActive()) {
            throw new BusinessRuleException("La institución debe estar activa antes de activar la promoción.");
        }
        if (active) {
            promotion.activate();
        } else {
            promotion.deactivate();
        }
        promotionRepository.save(promotion);
    }

    private SchoolPromotion getPromotionEntity(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la promoción escolar."));
    }

    private void requireAdministrator(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
        if (actor.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new BusinessRuleException("Solo un administrador puede gestionar promociones escolares.");
        }
    }
}
