package com.jairomatias.eventix.eligibility.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.category.entity.EventCategorySystemKey;
import com.jairomatias.eventix.eligibility.dto.SchoolAlumniBenefitConfiguration;
import com.jairomatias.eventix.eligibility.dto.SchoolAlumniCheckoutView;
import com.jairomatias.eventix.eligibility.dto.SchoolEligibilityResult;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitSystemKey;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupSystemKey;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.repository.EligibilityBenefitRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class SchoolAlumniBenefitService {

    private static final EligibilityGroupSystemKey GROUP_KEY =
            EligibilityGroupSystemKey.SCHOOL_ALUMNI;
    private static final EligibilityBenefitSystemKey BENEFIT_KEY =
            EligibilityBenefitSystemKey.SCHOOL_ALUMNI_DISCOUNT;

    private final EventRepository eventRepository;
    private final SchoolPromotionRepository promotionRepository;
    private final EligibilityGroupRepository groupRepository;
    private final EligibilityBenefitRepository benefitRepository;
    private final EligibilityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SchoolPromotionMembershipSyncService membershipSyncService;
    private final SchoolEligibilityService schoolEligibilityService;

    public SchoolAlumniBenefitService(
            EventRepository eventRepository,
            SchoolPromotionRepository promotionRepository,
            EligibilityGroupRepository groupRepository,
            EligibilityBenefitRepository benefitRepository,
            EligibilityMembershipRepository membershipRepository,
            UserRepository userRepository,
            SchoolPromotionMembershipSyncService membershipSyncService,
            SchoolEligibilityService schoolEligibilityService) {
        this.eventRepository = eventRepository;
        this.promotionRepository = promotionRepository;
        this.groupRepository = groupRepository;
        this.benefitRepository = benefitRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.membershipSyncService = membershipSyncService;
        this.schoolEligibilityService = schoolEligibilityService;
    }

    @Transactional(readOnly = true)
    public boolean isSchoolPromotionEvent(Long eventId) {
        return findEvent(eventId).getCategory().getSystemKey()
                == EventCategorySystemKey.SCHOOL_PROMOTION;
    }

    @Transactional
    public void reconcileAfterEventUpdate(
            Long eventId,
            String authenticatedLogin) {
        Event event = findEvent(eventId);
        authorizeManagement(event, findUser(authenticatedLogin));

        boolean benefitStillAllowed =
                event.getCategory().getSystemKey()
                        == EventCategorySystemKey.SCHOOL_PROMOTION
                && !event.isFreeEvent();
        if (benefitStillAllowed) {
            return;
        }

        groupRepository.findByEvent_IdAndSystemKey(eventId, GROUP_KEY)
                .filter(EligibilityGroup::isActive)
                .ifPresent(this::deactivateManagedGroupAndBenefit);
    }

    @Transactional
    public void configure(
            Long eventId,
            boolean enabled,
            Long promotionId,
            EligibilityBenefitType discountType,
            BigDecimal discountValue,
            String authenticatedLogin) {
        Event event = findEvent(eventId);
        authorizeManagement(event, findUser(authenticatedLogin));

        Optional<EligibilityGroup> managedGroup = groupRepository
                .findByEvent_IdAndSystemKey(eventId, GROUP_KEY);

        if (!enabled) {
            managedGroup.ifPresent(this::deactivateManagedGroupAndBenefit);
            return;
        }

        requireSchoolPromotionCategory(event);
        if (event.isFreeEvent()) {
            throw new BusinessRuleException(
                    "El descuento para egresados solo puede activarse en eventos de pago.");
        }
        validateDiscount(discountType, discountValue);
        SchoolPromotion promotion = findActivePromotion(promotionId);

        EligibilityGroup group = resolveManagedGroup(event, managedGroup, promotion);
        group.update(
                managedGroupName(promotion),
                EligibilityGroupType.PROMOTION_MEMBER,
                null,
                promotion);
        group.activate();
        EligibilityGroup persistedGroup = groupRepository.saveAndFlush(group);
        membershipSyncService.syncGroup(persistedGroup.getId());

        EligibilityBenefit benefit = benefitRepository
                .findByGroup_IdAndSystemKey(persistedGroup.getId(), BENEFIT_KEY)
                .orElseGet(() -> new EligibilityBenefit(
                        persistedGroup,
                        discountType,
                        discountValue,
                        null,
                        null,
                        null,
                        null,
                        BENEFIT_KEY));
        benefit.update(
                discountType,
                discountValue,
                null,
                null,
                null,
                null);
        benefit.activate();
        benefitRepository.save(benefit);
    }

    @Transactional(readOnly = true)
    public SchoolAlumniBenefitConfiguration getConfiguration(
            Long eventId,
            String authenticatedLogin) {
        Event event = findEvent(eventId);
        authorizeManagement(event, findUser(authenticatedLogin));

        Optional<EligibilityGroup> group = groupRepository
                .findByEvent_IdAndSystemKey(eventId, GROUP_KEY);
        if (group.isEmpty()) {
            return SchoolAlumniBenefitConfiguration.disabled();
        }

        Optional<EligibilityBenefit> benefit = benefitRepository
                .findByGroup_IdAndSystemKey(group.get().getId(), BENEFIT_KEY);
        Long promotionId = group.get().getSchoolPromotion() == null
                ? null
                : group.get().getSchoolPromotion().getId();
        EligibilityBenefitType discountType = benefit
                .map(EligibilityBenefit::getBenefitType)
                .orElse(EligibilityBenefitType.PERCENTAGE_DISCOUNT);
        BigDecimal discountValue = benefit
                .map(EligibilityBenefit::getDiscountValue)
                .orElse(null);
        boolean enabled = group.get().isActive()
                && benefit.map(EligibilityBenefit::isActive).orElse(false);

        return new SchoolAlumniBenefitConfiguration(
                enabled,
                promotionId,
                discountType,
                discountValue);
    }

    @Transactional(readOnly = true)
    public Optional<SchoolAlumniCheckoutView> getCheckoutView(
            Long eventId,
            String authenticatedLogin) {
        Event event = findEvent(eventId);
        if (event.getCategory().getSystemKey()
                != EventCategorySystemKey.SCHOOL_PROMOTION
                || event.isFreeEvent()) {
            return Optional.empty();
        }

        EligibilityGroup group = groupRepository
                .findByEvent_IdAndSystemKey(eventId, GROUP_KEY)
                .filter(EligibilityGroup::isActive)
                .orElse(null);
        if (group == null || group.getSchoolPromotion() == null) {
            return Optional.empty();
        }

        SchoolPromotion promotion = group.getSchoolPromotion();
        if (!promotion.isActive() || !promotion.getInstitution().isActive()) {
            return Optional.empty();
        }

        EligibilityBenefit benefit = benefitRepository
                .findByGroup_IdAndSystemKey(group.getId(), BENEFIT_KEY)
                .filter(EligibilityBenefit::isActive)
                .filter(item -> isSupportedDiscount(item.getBenefitType()))
                .orElse(null);
        if (benefit == null) {
            return Optional.empty();
        }

        User customer = findUser(authenticatedLogin);
        boolean verified = membershipRepository
                .findByGroup_IdAndUser_Id(group.getId(), customer.getId())
                .map(membership -> membership.isActive()
                        && membership.getStatus()
                                == EligibilityMembershipStatus.VERIFIED)
                .orElse(false);

        return Optional.of(new SchoolAlumniCheckoutView(
                promotion.getId(),
                promotion.getInstitution().getName(),
                promotion.getName(),
                promotion.getGraduationYear(),
                benefit.getBenefitType(),
                benefit.getDiscountValue(),
                verified));
    }

    @Transactional
    public SchoolEligibilityResult verifyForCheckout(
            Long eventId,
            String authenticatedLogin,
            String nationalId) {
        Event event = findEvent(eventId);
        requireSchoolPromotionCategory(event);
        if (event.isFreeEvent()) {
            throw new BusinessRuleException(
                    "Los eventos gratuitos no requieren descuento de egresados.");
        }
        User customer = findUser(authenticatedLogin);

        EligibilityGroup group = groupRepository
                .findByEvent_IdAndSystemKey(eventId, GROUP_KEY)
                .filter(EligibilityGroup::isActive)
                .orElseThrow(() -> new BusinessRuleException(
                        "Este evento no tiene un descuento de egresados activo."));
        if (group.getSchoolPromotion() == null) {
            throw new BusinessRuleException(
                    "El descuento de egresados no tiene una promoción escolar asociada.");
        }
        benefitRepository.findByGroup_IdAndSystemKey(group.getId(), BENEFIT_KEY)
                .filter(EligibilityBenefit::isActive)
                .filter(benefit -> isSupportedDiscount(
                        benefit.getBenefitType()))
                .orElseThrow(() -> new BusinessRuleException(
                        "Este evento no tiene un descuento monetario de egresados activo."));

        return schoolEligibilityService.verifyAndPersist(
                customer.getId(),
                group.getSchoolPromotion().getId(),
                nationalId);
    }

    private EligibilityGroup resolveManagedGroup(
            Event event,
            Optional<EligibilityGroup> managedGroup,
            SchoolPromotion promotion) {
        if (managedGroup.isEmpty()) {
            return new EligibilityGroup(
                    event,
                    managedGroupName(promotion),
                    EligibilityGroupType.PROMOTION_MEMBER,
                    null,
                    promotion,
                    GROUP_KEY);
        }

        EligibilityGroup current = managedGroup.get();
        Long currentPromotionId = current.getSchoolPromotion() == null
                ? null
                : current.getSchoolPromotion().getId();
        if (promotion.getId().equals(currentPromotionId)) {
            return current;
        }

        deactivateManagedGroupAndBenefit(current);
        current.releaseSystemKey();
        groupRepository.saveAndFlush(current);
        return new EligibilityGroup(
                event,
                managedGroupName(promotion),
                EligibilityGroupType.PROMOTION_MEMBER,
                null,
                promotion,
                GROUP_KEY);
    }

    private void deactivateManagedGroupAndBenefit(EligibilityGroup group) {
        group.deactivate();
        benefitRepository.findByGroup_IdAndSystemKey(group.getId(), BENEFIT_KEY)
                .ifPresent(benefit -> {
                    benefit.deactivate();
                    benefitRepository.save(benefit);
                });
        groupRepository.save(group);
    }

    private void validateDiscount(
            EligibilityBenefitType discountType,
            BigDecimal discountValue) {
        if (!isSupportedDiscount(discountType)) {
            throw new BusinessRuleException(
                    "El descuento de egresados debe ser porcentual o de monto fijo.");
        }
        if (discountValue == null
                || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "El valor del descuento para egresados debe ser mayor que cero.");
        }
        if (discountType == EligibilityBenefitType.PERCENTAGE_DISCOUNT
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException(
                    "El descuento porcentual para egresados no puede superar 100%.");
        }
    }

    private boolean isSupportedDiscount(EligibilityBenefitType type) {
        return type == EligibilityBenefitType.PERCENTAGE_DISCOUNT
                || type == EligibilityBenefitType.FIXED_DISCOUNT;
    }

    private SchoolPromotion findActivePromotion(Long promotionId) {
        if (promotionId == null) {
            throw new BusinessRuleException(
                    "Selecciona la promoción escolar cuyo padrón validará el descuento.");
        }
        SchoolPromotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la promoción escolar seleccionada."));
        if (!promotion.isActive() || !promotion.getInstitution().isActive()) {
            throw new BusinessRuleException(
                    "La promoción escolar y su institución deben estar activas.");
        }
        return promotion;
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el evento solicitado."));
    }

    private User findUser(String login) {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        login,
                        login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private void authorizeManagement(Event event, User actor) {
        RoleName role = actor.getRole().getName();
        boolean administrator = role == RoleName.ADMINISTRATOR;
        boolean owner = role == RoleName.ORGANIZER
                && event.getOrganizer().getId().equals(actor.getId());
        if (!administrator && !owner) {
            throw new BusinessRuleException(
                    "No tienes permisos para configurar el descuento de egresados de este evento.");
        }
    }

    private void requireSchoolPromotionCategory(Event event) {
        if (event.getCategory().getSystemKey()
                != EventCategorySystemKey.SCHOOL_PROMOTION) {
            throw new BusinessRuleException(
                    "El descuento de egresados solo puede configurarse en eventos de Promoción escolar.");
        }
    }

    private String managedGroupName(SchoolPromotion promotion) {
        return "Egresados · " + promotion.getName() + " "
                + promotion.getGraduationYear();
    }
}
