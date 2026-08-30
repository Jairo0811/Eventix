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
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.service.InstitutionAuthorizationService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;

@Service
public class SchoolPromotionManagementService {

    private final SchoolInstitutionRepository institutionRepository;
    private final SchoolPromotionRepository promotionRepository;
    private final PromotionMemberRepository memberRepository;
    private final SchoolRosterImportRepository rosterImportRepository;
    private final EligibilityVerificationRepository verificationRepository;
    private final InstitutionAuthorizationService authorizationService;

    public SchoolPromotionManagementService(
            SchoolInstitutionRepository institutionRepository,
            SchoolPromotionRepository promotionRepository,
            PromotionMemberRepository memberRepository,
            SchoolRosterImportRepository rosterImportRepository,
            EligibilityVerificationRepository verificationRepository,
            InstitutionAuthorizationService authorizationService) {
        this.institutionRepository = institutionRepository;
        this.promotionRepository = promotionRepository;
        this.memberRepository = memberRepository;
        this.rosterImportRepository = rosterImportRepository;
        this.verificationRepository = verificationRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<SchoolInstitutionView> listInstitutions(Long actorId) {
        authorizationService.requireAdministrator(actorId);
        return institutionRepository.findAllByOrderByNameAsc().stream()
                .map(SchoolInstitutionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPromotionView> listPromotions(Long actorId) {
        authorizationService.requireAdministrator(actorId);
        return promotionRepository.findAllByOrderByGraduationYearDescNameAsc().stream()
                .map(SchoolPromotionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPromotionView> listPromotionsForInstitution(Long institutionId, Long actorId) {
        SchoolInstitution institution = getInstitutionEntity(institutionId);
        authorizationService.requireInstitutionRole(institution, actorId);
        return promotionRepository.findAllByInstitution_IdOrderByGraduationYearDescNameAsc(institutionId)
                .stream()
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
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        authorizationService.requireInstitutionRole(promotion.getInstitution(), actorId);
        return SchoolPromotionView.from(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionMemberView> listMembers(Long promotionId, Long actorId) {
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        authorizationService.requireInstitutionRole(promotion.getInstitution(), actorId);
        return memberRepository.findAllByPromotion_IdOrderByFullNameAsc(promotionId).stream()
                .map(PromotionMemberView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RosterImportView> listImports(Long promotionId, Long actorId) {
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        authorizationService.requireInstitutionRole(promotion.getInstitution(), actorId);
        return rosterImportRepository.findAllByPromotion_IdOrderByImportedAtDesc(promotionId).stream()
                .map(RosterImportView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolVerificationView> listVerifications(Long actorId) {
        authorizationService.requireAdministrator(actorId);
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
        authorizationService.requireAdministrator(actorId);
        String code = form.code().trim().toUpperCase();
        if (institutionRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Ya existe una institución con ese código.");
        }
        return institutionRepository.save(new SchoolInstitution(form.name(), code)).getId();
    }

    @Transactional
    public void updateInstitution(Long institutionId, SchoolInstitutionForm form, Long actorId) {
        authorizationService.requireAdministrator(actorId);
        SchoolInstitution institution = getInstitutionEntity(institutionId);
        String code = form.code().trim().toUpperCase();
        if (institutionRepository.existsByCodeIgnoreCaseAndIdNot(code, institutionId)) {
            throw new BusinessRuleException("Ya existe otra institución con ese código.");
        }
        institution.update(form.name(), code);
        institutionRepository.save(institution);
    }

    @Transactional
    public void setInstitutionActive(Long institutionId, boolean active, Long actorId) {
        authorizationService.requireAdministrator(actorId);
        SchoolInstitution institution = getInstitutionEntity(institutionId);
        if (active) {
            institution.activate();
        } else {
            institution.deactivate();
        }
        institutionRepository.save(institution);
    }

    @Transactional
    public Long createPromotion(SchoolPromotionForm form, Long actorId) {
        SchoolInstitution institution = getInstitutionEntity(form.institutionId());
        authorizationService.requireOperationalRole(
                institution,
                actorId,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN,
                InstitutionMembershipRole.EVENT_MANAGER);
        if (promotionRepository.existsByInstitution_IdAndGraduationYear(
                form.institutionId(), form.graduationYear())) {
            throw new BusinessRuleException("Ya existe una promoción para ese año en la institución.");
        }
        SchoolPromotion promotion = new SchoolPromotion(institution, form.name(), form.graduationYear());
        return promotionRepository.save(promotion).getId();
    }

    @Transactional
    public void updatePromotion(Long promotionId, SchoolPromotionForm form, Long actorId) {
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        authorizationService.requireOperationalRole(
                promotion.getInstitution(),
                actorId,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN,
                InstitutionMembershipRole.EVENT_MANAGER);
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
        SchoolPromotion promotion = getPromotionEntity(promotionId);
        authorizationService.requireOperationalRole(
                promotion.getInstitution(),
                actorId,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN,
                InstitutionMembershipRole.EVENT_MANAGER);
        if (active) {
            promotion.activate();
        } else {
            promotion.deactivate();
        }
        promotionRepository.save(promotion);
    }

    private SchoolInstitution getInstitutionEntity(Long institutionId) {
        return institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la institución."));
    }

    private SchoolPromotion getPromotionEntity(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la promoción escolar."));
    }
}
