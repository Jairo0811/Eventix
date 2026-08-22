package com.jairomatias.eventix.eligibility.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.EligibilityGroupForm;
import com.jairomatias.eventix.eligibility.dto.EligibilityGroupView;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
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

    public EligibilityGroupManagementService(
            EligibilityGroupRepository groupRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibilityGroupView> list(Long eventId, Long actorId) {
        Event event = getEvent(eventId);
        authorize(actorId, event);
        return groupRepository.findAllByEvent_IdOrderByNameAsc(eventId)
                .stream()
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

        EligibilityGroup group = new EligibilityGroup(
                event,
                normalizedName,
                form.groupType(),
                form.maxRelatedPeople());
        return groupRepository.save(group).getId();
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

        group.update(normalizedName, form.groupType(), form.maxRelatedPeople());
        groupRepository.save(group);
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
