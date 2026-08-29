package com.jairomatias.eventix.eligibility.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.EligibilityGroupForm;
import com.jairomatias.eventix.eligibility.dto.EligibilityGroupView;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class EligibilityGroupManagementService {

    private final EligibilityGroupRepository groupRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SchoolPromotionRepository schoolPromotionRepository;
    private final SchoolPromotionMembershipSyncService membershipSyncService;

    public EligibilityGroupManagementService(
            EligibilityGroupRepository groupRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            SchoolPromotionRepository schoolPromotionRepository,
            SchoolPromotionMembershipSyncService membershipSyncService) {
        this.groupRepository = groupRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.schoolPromotionRepository = schoolPromotionRepository;
        this.membershipSyncService = membershipSyncService;
    }

    @Transactional(readOnly = true)
    public List<EligibilityGroupView> list(Long eventId, Long actorId) {
        Event event = getEvent(eventId);
        authorize(actorId, event);
        return groupRepository.findAllByEvent_IdOrderByNameAsc(eventId).stream()
                .map(EligibilityGroupView::from)
                .toList();
    }

    @Transactional
    public Long create(Long eventId, EligibilityGroupForm form, Long actorId) {
        Event event = getEvent(eventId);
        authorize(actorId, event);
        validateForm(form);
        String normalizedName = form.name().trim();
        if (groupRepository.existsByEvent_IdAndNameIgnoreCase(eventId, normalizedName)) {
            throw new BusinessRuleException("Ya existe un grupo de elegibilidad con ese nombre en el evento.");
        }
        SchoolPromotion schoolPromotion = resolveSchoolPromotion(form);
        EligibilityGroup group = new EligibilityGroup(
                event, normalizedName, form.groupType(), form.maxRelatedPeople(), schoolPromotion);
        Long id = groupRepository.save(group).getId();
        if (schoolPromotion != null) {
            membershipSyncService.syncGroup(id);
        }
        return id;
    }

    @Transactional
    public void update(Long groupId, EligibilityGroupForm form, Long actorId) {
        EligibilityGroup group = getGroupForUpdate(groupId);
        authorize(actorId, group.getEvent());
        validateForm(form);
        String normalizedName = form.name().trim();
        if (groupRepository.existsByEvent_IdAndNameIgnoreCaseAndIdNot(
                group.getEvent().getId(), normalizedName, groupId)) {
            throw new BusinessRuleException("Ya existe otro grupo de elegibilidad con ese nombre en el evento.");
        }
        SchoolPromotion schoolPromotion = resolveSchoolPromotion(form);
        if (group.getSchoolPromotion() != null
                && (schoolPromotion == null
                        || !group.getSchoolPromotion().getId().equals(schoolPromotion.getId()))) {
            throw new BusinessRuleException(
                    "Una promoción escolar vinculada no puede cambiarse; crea otro grupo para preservar la trazabilidad.");
        }
        group.update(normalizedName, form.groupType(), form.maxRelatedPeople(), schoolPromotion);
        groupRepository.save(group);
        if (schoolPromotion != null) {
            membershipSyncService.syncGroup(groupId);
        }
    }

    @Transactional
    public void setActive(Long groupId, boolean active, Long actorId) {
        EligibilityGroup group = getGroupForUpdate(groupId);
        authorize(actorId, group.getEvent());
        if (active) {
            group.activate();
        } else {
            group.deactivate();
        }
        groupRepository.save(group);
        if (active && group.getSchoolPromotion() != null) {
            membershipSyncService.syncGroup(groupId);
        }
    }

    private SchoolPromotion resolveSchoolPromotion(EligibilityGroupForm form) {
        if (form.groupType() != EligibilityGroupType.PROMOTION_MEMBER) {
            return null;
        }
        if (form.schoolPromotionId() == null) {
            throw new BusinessRuleException(
                    "Los grupos PROMOTION_MEMBER deben vincularse a una promoción escolar.");
        }
        SchoolPromotion promotion = schoolPromotionRepository.findById(form.schoolPromotionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la promoción escolar."));
        if (!promotion.isActive() || !promotion.getInstitution().isActive()) {
            throw new BusinessRuleException("La promoción escolar seleccionada debe estar activa.");
        }
        return promotion;
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
    }

    private EligibilityGroup getGroupForUpdate(Long groupId) {
        return groupRepository.findDetailedByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo de elegibilidad."));
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

    private void validateForm(EligibilityGroupForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new BusinessRuleException("El nombre del grupo es obligatorio.");
        }
        if (form.groupType() == null) {
            throw new BusinessRuleException("El tipo de grupo es obligatorio.");
        }
        if (form.maxRelatedPeople() != null && form.maxRelatedPeople() < 0) {
            throw new BusinessRuleException("El límite de familiares no puede ser negativo.");
        }
    }
}
