package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.dto.EligibilityGroupForm;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EligibilityGroupManagementServiceTest {

    @Mock
    private EligibilityGroupRepository groupRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolPromotionRepository schoolPromotionRepository;
    @Mock
    private SchoolPromotionMembershipSyncService membershipSyncService;

    private EligibilityGroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityGroupManagementService(
                groupRepository,
                eventRepository,
                userRepository,
                schoolPromotionRepository,
                membershipSyncService);
    }

    @Test
    void organizerCanCreateGroupForOwnEvent() {
        Event event = eventOwnedBy(20L);
        User organizer = user(RoleName.ORGANIZER);
        EligibilityGroupForm form = new EligibilityGroupForm("Egresados 2020", EligibilityGroupType.ALUMNI, null);

        when(eventRepository.findDetailedById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(organizer));
        when(groupRepository.existsByEvent_IdAndNameIgnoreCase(100L, "Egresados 2020")).thenReturn(false);
        when(groupRepository.save(any(EligibilityGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(100L, form, 20L);

        verify(groupRepository).save(any(EligibilityGroup.class));
    }

    @Test
    void organizerCannotManageAnotherOrganizersEvent() {
        Event event = eventOwnedBy(99L);
        User organizer = user(RoleName.ORGANIZER);
        EligibilityGroupForm form = new EligibilityGroupForm("VIP", EligibilityGroupType.VIP, null);

        when(eventRepository.findDetailedById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(organizer));

        assertThatThrownBy(() -> service.create(100L, form, 20L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permisos");

        verify(groupRepository, never()).save(any());
    }

    @Test
    void duplicateGroupNameIsRejectedWithinSameEvent() {
        Event event = mock(Event.class);
        User administrator = user(RoleName.ADMINISTRATOR);
        EligibilityGroupForm form = new EligibilityGroupForm("Staff", EligibilityGroupType.STAFF, null);

        when(eventRepository.findDetailedById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));
        when(groupRepository.existsByEvent_IdAndNameIgnoreCase(100L, "Staff")).thenReturn(true);

        assertThatThrownBy(() -> service.create(100L, form, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe");

        verify(groupRepository, never()).save(any());
    }

    @Test
    void promotionMemberGroupRequiresSchoolPromotion() {
        Event event = mock(Event.class);
        User administrator = user(RoleName.ADMINISTRATOR);
        EligibilityGroupForm form = new EligibilityGroupForm(
                "Promoción 2017", EligibilityGroupType.PROMOTION_MEMBER, null, null);

        when(eventRepository.findDetailedById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));
        when(groupRepository.existsByEvent_IdAndNameIgnoreCase(100L, "Promoción 2017")).thenReturn(false);

        assertThatThrownBy(() -> service.create(100L, form, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("promoción escolar");

        verify(groupRepository, never()).save(any());
    }

    @Test
    void promotionMemberGroupBackfillsVerifiedSchoolMembers() {
        Event event = mock(Event.class);
        User administrator = user(RoleName.ADMINISTRATOR);
        SchoolInstitution institution = new SchoolInstitution("Colegio Demo", "DEMO");
        SchoolPromotion promotion = new SchoolPromotion(institution, "Promoción 2017", 2017);
        EligibilityGroup saved = mock(EligibilityGroup.class);
        when(saved.getId()).thenReturn(300L);
        EligibilityGroupForm form = new EligibilityGroupForm(
                "Promoción 2017", EligibilityGroupType.PROMOTION_MEMBER, null, 10L);

        when(eventRepository.findDetailedById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));
        when(groupRepository.existsByEvent_IdAndNameIgnoreCase(100L, "Promoción 2017")).thenReturn(false);
        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(groupRepository.save(any(EligibilityGroup.class))).thenReturn(saved);

        service.create(100L, form, 1L);

        verify(membershipSyncService).syncGroup(300L);
    }

    private Event eventOwnedBy(Long organizerId) {
        Event event = mock(Event.class);
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(organizerId);
        when(event.getOrganizer()).thenReturn(owner);
        return event;
    }

    private User user(RoleName roleName) {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(roleName);
        return user;
    }
}
